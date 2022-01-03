package electionguard

import FinalizationRegistry
import FinalizationRegistryI
import gmpwasm.GMPInterface
import gmpwasm.getGMPInterface
import gmpwasm.mpz_ptr
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.Promise

// Memory management: this is something we have to do carefully for gmp-wasm, which is
// completely unnecessary when dealing with the JVM. At the lowest level, we have
// calls to mpz_init(), which allocates memory in the gmp-wasm context, returning
// an mpz_t pointer, and we have mpz_t_free(), which deallocates that same memory.

// We're going to use a relatively new JavaScript feature, finalizers, to deal with
// this for us. An explicit goal is to avoid too much copying back and forth from
// the wasm environment to the JavaScript environment.

/**
 * Call this first. Returns a promise to our `GmpContext` class, which then has
 */
fun getGmpContext(): Promise<GmpContext> = getGMPInterface().then { GmpContext(it) }

/**
 * This class wraps the `GMPInterface` provided by `gmp-wasm` and gives us a
 * simple BigInteger abstraction that efficiently uses the underlying WASM
 * implementation, and leverages the latest JavaScript "finalization" features
 * to help us figure out when it's time to free the underlying `mpz_ptr` values
 * living in the WASM memory.
 */
class GmpContext(val gmp: GMPInterface) {
    private val registry: FinalizationRegistryI = FinalizationRegistry {
        val tmp = it as mpz_ptr
        gmp.mpz_clear(tmp) // frees GnuMP internal memory
        gmp.mpz_t_free(tmp) // frees the mpz_t wrapper
    }

    /** Helper function: allocates an `mpz` and initializes it to zero. */
    internal fun newEmpty(): mpz_ptr {
        val tmp = gmp.mpz_t()
        gmp.mpz_init(tmp)
        return tmp
    }

    /**
     * Creates a BigInteger wrapper around the passed mpz_ptr, ensuring that
     * when the wrapper becomes garbage, the contained mpz_ptr will be freed.
     */
    internal fun wrap(mpz: mpz_ptr): BigInteger {
        val result = BigInteger(mpz, this)
        registry.register(result, mpz)
        return result
    }

    /** Converts the given constant to a BigInteger. */
    fun constant(i: Number): BigInteger {
        val result: mpz_ptr = gmp.mpz_t()
        gmp.mpz_set_ui(result, i)
        return wrap(result)
    }

    /** Converts the given big-endian byte array representation to a BigInteger. */
    fun fromByteArray(byteArray: ByteArray): BigInteger {
        val ua = Uint8Array(byteArray.size)
        for (i in 0..byteArray.size - 1) {
            ua[i] = byteArray[i]
        }
        return fromBytes(ua)
    }

    /** Converts the given big-endian byte array representation to a BigInteger. */
    fun fromBytes(byteArray: Uint8Array): BigInteger {
        val result: mpz_ptr = gmp.mpz_t()

        val wasmBuf = gmp.malloc(byteArray.length)
        gmp.mem.set(byteArray, offset = wasmBuf as Int)
        gmp.mpz_import(result, byteArray.length, 1, 1, 1, 0, wasmBuf)
        gmp.free(wasmBuf)
        return wrap(result)
    }

    val zero = lazy { wrap(0) }
    val one = lazy { wrap(1) }
    val two = lazy { wrap(2) }
}

/** A minimal BigInteger-style wrapper around GMP-Wasm. */
class BigInteger(val mpz: mpz_ptr, val context: GmpContext): Comparable<BigInteger> {
    operator fun plus(other: BigInteger): BigInteger {
        val result = context.newEmpty()
        context.gmp.mpz_add(result, this.mpz, other.mpz)
        return context.wrap(result)
    }

    operator fun minus(other: BigInteger): BigInteger {
        val result = context.newEmpty()
        context.gmp.mpz_sub(result, this.mpz, other.mpz)
        return context.wrap(result)
    }

    operator fun times(other: BigInteger): BigInteger {
        val result = context.newEmpty()
        context.gmp.mpz_mul(result, this.mpz, other.mpz)
        return context.wrap(result)
    }

    operator fun rem(other: BigInteger): BigInteger {
        val result = context.newEmpty()
        context.gmp.mpz_mod(result, this.mpz, other.mpz)
        return context.wrap(result)
    }

    fun modPow(exp: BigInteger, modulus: BigInteger): BigInteger {
        val result = context.newEmpty()

        // there's a "secure" version of this, but we're not worried about timing attacks
        context.gmp.mpz_powm(result, this.mpz, exp.mpz, modulus.mpz)
        return context.wrap(result)
    }

    fun modInverse(modulus: BigInteger): BigInteger {
        val result = context.newEmpty()
        context.gmp.mpz_invert(result, this.mpz, modulus.mpz)
        return context.wrap(result)
    }

    /** Return the position of the most significant bit. (LSB = 0) */
    fun msbPosition(): Int = (context.gmp.mpz_sizeinbase(this.mpz, 2) as Int) - 1

    /** Returns a big-endian byte array. */
    fun toBytes(): Uint8Array {
        //     /** Exports integer into an Uint8Array. Sign is ignored. */
        //    toBuffer(littleEndian = false): Uint8Array {
        //      const countPtr = gmp.malloc(4);
        //      const startptr = gmp.mpz_export(0, countPtr, littleEndian ? -1 : 1, 1, 1, 0, this.mpz_t);
        //      const size = gmp.memView.getUint32(countPtr, true);
        //      const endptr = startptr + size;
        //      const buf = gmp.mem.slice(startptr, endptr);
        //      gmp.free(startptr);
        //      gmp.free(countPtr);
        //      return buf;
        //    },
    }

    override fun compareTo(other: BigInteger): Int {
        // Happily, mpz's idea of comparison is exactly the same as Java / Kotlin,
        // so we get all the usual comparison operators for very little work.
        return context.gmp.mpz_cmp(this.mpz, other.mpz) as Int
    }

    override fun toString(): String {
        // Here's the original TypeScript solution to this. Sadly, the indexOf()
        // method isn't defined in the Kotlin version of Uint8Array (why?) and
        // neither is the string decoder. Since we don't care about the performance,
        // just the correctness, we're going to load the bytes, one by one, into
        // a mutable array, and just use Kotlin's joinToString() method instead.

        //      const strptr = gmp.mpz_get_str(0, radix, this.mpz_t);
        //      const endptr = gmp.mem.indexOf(0, strptr);
        //      const str = decoder.decode(gmp.mem.subarray(strptr, endptr));
        //      gmp.free(strptr);

        val strptr = context.gmp.mpz_get_str(0, 10, this.mpz)
        var offset = strptr as Int
        val strBytes = mutableListOf<Byte>()
        while(context.gmp.mem[offset] != 0.toByte()) {
            strBytes.add(context.gmp.mem[offset])
            offset++
        }

        val result = strBytes.joinToString(separator = "")
        context.gmp.free(strptr)

        return result
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun equals(other: Any?) = when (other) {
        is BigInteger -> this.compareTo(other) == 0
        else -> false
    }
}