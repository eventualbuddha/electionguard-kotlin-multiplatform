package electionguard

import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.Promise

@JsModule("gmp-wasm")

external enum class DivMode

/**
 * Simplified interface to gmp-wasm's Integer class. The real one is capable of taking JavaScript
 * numbers and other types as arguments, while our interface insists that everything is an Integer.
 */
external class Integer {
    val mpz_t: Any

    /** Returns the sum of this number and the given one. */
    fun add(v: Integer): Integer

    /** Returns the difference of this number and the given one. */
    fun sub(v: Integer): Integer

    /** Returns the product of this number and the given one. */
    fun mul(v: Integer): Integer

    /** Returns the number with inverted sign. */
    fun neg(): Integer

    /** Returns the absolute value of this number. */
    fun abs(): Integer

    /**
     * Returns the result of the division of this number by the given one. The `mode` parameter
     * defaults to `DivMode.CEIL`.
     */
    fun div(v: Integer, mode: DivMode = definedExternally): Integer

    /** Returns this number exponentiated to the given value. */
    fun pow(v: Integer): Integer

    /** Returns this number exponentiated to the given value, mod the given value. */
    fun pow(v: Integer, mod: Integer): Integer

    /** Returns the integer square root number of this number, rounded down. */
    fun sqrt(): Integer

    fun nthRoot(nth: Int): Integer

    fun factorial(): Integer

    fun doubleFactorial(): Integer

    /** Returns false if definitely not prime, true if definitely prime, or "probably-prime" otherwise. */
    fun isPrime(reps: Int = definedExternally): Any /* Boolean | String */

    fun nextPrime(): Integer

    /** Returns the greatest common divisor of this number and the given one. */
    fun gcd(v: Integer): Integer

    fun lcm(v: Integer): Integer

    fun complement1(): Integer

    fun complement2(): Integer

    /** Returns the integer bitwise-and combined with another integer. */
    fun and(v: Integer): Integer

    /** Returns the integer bitwise-or combined with another integer. */
    fun or(v: Integer): Integer

    /** Returns the integer bitwise-xor combined with another integer. */
    fun xor(v: Integer): Integer

    /** Returns the integer left shifted by a given number of bits. */
    fun shiftLeft(v: Int): Integer

    /** Returns the integer right shifted by a given number of bits. */
    fun shiftRight(v: Int): Integer

    /** Sets the value of bit i to 1. The least significant bit is number 0 */
    fun setBit(i: Int): Integer

    /** Sets the value of multiple bits to 1. The least significant bit is number 0 */
    fun setBits(indices: IntArray): Integer

    /** Sets the value of bit i to 0. The least significant bit is number 0 */
    fun clearBit(index: Int): Integer

    /** Sets the value of multiple bits to 0. The least significant bit is number 0 */
    fun clearBits(indices: IntArray): Integer

    /** Inverts the value of bit i. The least significant bit is number 0 */
    fun flipBit(index: Int): Integer

    /** Inverts the value of multiple bits. The least significant bit is number 0 */
    fun flipBits(indices: IntArray): Integer

    /** Returns 0 or 1 based on the value of a bit at the provided index. The least significant bit is number 0 */
    fun getBit(index: Int): Int

    // Returns the position of the most significant bit. The least significant bit is number 0.
    fun msbPosition(): Int

    /** Works similarly to JS Array.slice() but on bits. The least significant bit is number 0 */
    fun sliceBits(start: Int, end: Int): Integer

    fun isEqual(v: Integer): Boolean

    fun lessThan(v: Integer): Boolean

    fun lessOrEqual(v: Integer): Boolean

    fun greaterThan(v: Integer): Boolean

    fun greaterOrEqual(v: Integer): Boolean

    fun sign(): Int

    /** Exports integer into an Uint8Array. Sign is ignored. */
    fun toBuffer(littleEndian: Boolean): Uint8Array

    fun toString(radix: Int = definedExternally): String
}

fun Integer.mod(g: GMPLib, v: Integer): Integer {
    val result = g.ctx.Integer()
    g.binding.mpz_mod(result.mpz_t, this.mpz_t, v.mpz_t)
    return result
}

fun Integer.divMod(g: GMPLib, v: Integer, m: Integer) =
    this.timesMod(g, v.multiplicativeInverse(g, m), m)

fun Integer.plusMod(g: GMPLib, v: Integer, m: Integer) = add(v).mod(g, m)
fun Integer.minusMod(g: GMPLib, v: Integer, m: Integer) = sub(v).mod(g, m)
fun Integer.timesMod(g: GMPLib, v: Integer, m: Integer) = mul(v).mod(g, m)
fun Integer.powMod(g: GMPLib, v: Integer, m: Integer) = pow(v, m)
fun Integer.multiplicativeInverse(g: GMPLib, m: Integer): Integer {
    val result = g.ctx.Integer()
    val success = g.binding.mpz_invert(result.mpz_t, this.mpz_t, m.mpz_t)
    if (success != 0) {
        return result
    } else {
        throw IllegalStateException("No multiplicative inverse")
    }
}

typealias mpz_ptr = Any
typealias mpz_srcptr = Any
typealias c_int = Int

/**
 * Just a subset of the functions that we're actually going to need. The full list
 * can be found in the original code and can be expanded, as necessary.
 * https://github.com/Daninet/gmp-wasm/blob/master/src/functions.ts
 */
external class GMPFunctions {
    /** Set r to n mod d. */
    fun mpz_mod(r: mpz_ptr, n: mpz_srcptr, d: mpz_srcptr)

    /**
     * Compute the inverse of op1 modulo op2 and put the result in rop.
     * If the inverse exists, the return value is non-zero.
     * If an inverse doesn’t exist the return value is zero and rop is undefined.
     */
    fun mpz_invert(rop: mpz_ptr, op1: mpz_srcptr, op2: mpz_srcptr): c_int
}

external interface CalculateTypeWithDestroy {
    fun Integer(): Integer
    fun Integer(value: dynamic): Integer
    fun isInteger(value: dynamic): Boolean
    fun destroy()

}

external class GMPLib {
    val binding: GMPFunctions
    fun calculate(fn: (GMPFunctions) -> Integer): String
    fun getContext(): CalculateTypeWithDestroy
    fun reset(): Promise<Unit>
}

val GMPLib.ctx: CalculateTypeWithDestroy
    get() = getContext()

external suspend fun init(): Promise<GMPLib>

/**
 * Loads Gmp, if it's not already loaded, and returns a handle to `GMPLib`, which is
 * used as a context-style argument to a variety of GMP functions.
 */
fun getGmpLib() = getPromise { init() }

fun ByteArray.toUint8Array(): Uint8Array {
    val result = Uint8Array(this.size)
    for (i in 0 until this.size) {
        result.set(i, this[i])
    }
    return result
}

fun Uint8Array.toByteArray() = ByteArray(this.length) {
    this.get(it)
}

fun ByteArray.toBigInteger(g: GMPLib): Integer {
    return g.ctx.Integer(this.toUint8Array())
}

fun Integer.toByteArray(g: GMPLib) = toBuffer(false).toByteArray()

fun Int.toBigInteger(g: GMPLib) = this.toLong().toBigInteger(g)
fun UInt.toBigInteger(g: GMPLib) = this.toLong().toBigInteger(g)
fun Long.toBigInteger(g: GMPLib): Integer {
    val bytes = ByteArray(8) {
        // big-endian
        ((this shr (8 * (7 - it))) and 0xFF).toByte()
    }
    return bytes.toBigInteger(g)
}