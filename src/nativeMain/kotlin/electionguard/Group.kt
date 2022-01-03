@file:OptIn(ExperimentalUnsignedTypes::class)
@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED") // fix IntelliJ confusion

package electionguard

import electionguard.Base64.fromSafeBase64
import hacl.*
import kotlinx.cinterop.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import platform.posix.free

private val testGroupContext =
    GroupContext(
        pBytes = b64TestP.fromSafeBase64(),
        qBytes = b64TestQ.fromSafeBase64(),
        p256minusQBytes = b64TestP256MinusQ.fromSafeBase64(),
        gBytes = b64TestG.fromSafeBase64(),
        rBytes = b64TestR.fromSafeBase64(),
        strong = false,
        name = "16-bit test group",
        powRadixOption = PowRadixOption.NO_ACCELERATION
    )

private val productionGroups =
    PowRadixOption.values().associateWith {
        GroupContext(
            pBytes = b64ProductionP.fromSafeBase64(),
            qBytes = b64ProductionQ.fromSafeBase64(),
            p256minusQBytes = b64ProductionP256MinusQ.fromSafeBase64(),
            gBytes = b64ProductionG.fromSafeBase64(),
            rBytes = b64ProductionR.fromSafeBase64(),
            strong = true,
            name = "production group, ${it.description}",
            powRadixOption = it
        )
    }

actual suspend fun productionGroup(acceleration: PowRadixOption) : GroupContext =
    productionGroups[acceleration] ?: throw Error("can't happen")

actual suspend fun testGroup() = testGroupContext

typealias HaclBignum4096 = ULongArray
typealias HaclBignum256 = ULongArray

internal const val HaclBignum256_LongWords = 4
internal const val HaclBignum4096_LongWords = 64
internal const val HaclBignum256_Bytes = HaclBignum256_LongWords * 8
internal const val HaclBignum4096_Bytes = HaclBignum4096_LongWords * 8

internal fun newZeroBignum4096() = HaclBignum4096(HaclBignum4096_LongWords)
internal fun newZeroBignum256() = HaclBignum256(HaclBignum256_LongWords)

// helper functions that make it less awful to go back and forth from Kotlin to C interaction

internal inline fun <T> nativeElems(a: ULongArray,
                                    b: ULongArray,
                                    c: ULongArray,
                                    d: ULongArray,
                                    e: ULongArray,
                                    f: (ap: CPointer<ULongVar>, bp: CPointer<ULongVar>, cp: CPointer<ULongVar>, dp: CPointer<ULongVar>, ep: CPointer<ULongVar>) -> T): T =
    a.useNative { ap -> b.useNative { bp -> c.useNative { cp -> d.useNative { dp -> e.useNative { ep -> f(ap, bp, cp, dp, ep) } } } } }

internal inline fun <T> nativeElems(a: ULongArray,
                                    b: ULongArray,
                                    c: ULongArray,
                                    d: ULongArray,
                                    f: (ap: CPointer<ULongVar>, bp: CPointer<ULongVar>, cp: CPointer<ULongVar>, dp: CPointer<ULongVar>) -> T): T =
    a.useNative { ap -> b.useNative { bp -> c.useNative { cp -> d.useNative { dp -> f(ap, bp, cp, dp) } } } }

internal inline fun <T> nativeElems(a: ULongArray,
                                    b: ULongArray,
                                    c: ULongArray,
                                    f: (ap: CPointer<ULongVar>, bp: CPointer<ULongVar>, cp: CPointer<ULongVar>) -> T): T =
    a.useNative { ap -> b.useNative { bp -> c.useNative { cp -> f(ap, bp, cp) } } }

internal inline fun <T> nativeElems(a: ULongArray,
                                    b: ULongArray,
                                    f: (ap: CPointer<ULongVar>, bp: CPointer<ULongVar>) -> T): T =
    a.useNative { ap -> b.useNative { bp -> f(ap, bp) } }

internal inline fun <T> nativeElems(a: ULongArray,
                                    f: (ap: CPointer<ULongVar>) -> T): T =
    a.useNative { ap -> f(ap) }

internal inline fun <T> ULongArray.useNative(f: (CPointer<ULongVar>) -> T): T =
    usePinned { ptr ->
        f(ptr.addressOf(0).reinterpret())
    }

internal inline fun <T> ByteArray.useNative(f: (CPointer<UByteVar>) -> T): T =
    usePinned { ptr ->
        f(ptr.addressOf(0).reinterpret())
    }


internal fun UInt.toHaclBignum256(): HaclBignum256 {
    val bytes = ByteArray(HaclBignum256_Bytes)
    // big-endian
    bytes[HaclBignum256_Bytes - 4] = ((this and 0xff_00_00_00U) shr 24).toByte()
    bytes[HaclBignum256_Bytes - 3] = ((this and 0xff_00_00U) shr 16).toByte()
    bytes[HaclBignum256_Bytes - 2] = ((this and 0xff_00U) shr 8).toByte()
    bytes[HaclBignum256_Bytes - 1] = ((this and 0xffU)).toByte()
    return bytes.toHaclBignum256()
}

internal fun UInt.toHaclBignum4096(): HaclBignum4096 {
    val bytes = ByteArray(HaclBignum4096_Bytes)
    // big-endian
    bytes[HaclBignum4096_Bytes - 4] = ((this and 0xff_00_00_00U) shr 24).toByte()
    bytes[HaclBignum4096_Bytes - 3] = ((this and 0xff_00_00U) shr 16).toByte()
    bytes[HaclBignum4096_Bytes - 2] = ((this and 0xff_00U) shr 8).toByte()
    bytes[HaclBignum4096_Bytes - 1] = ((this and 0xffU)).toByte()
    return bytes.toHaclBignum4096()
}

/** Convert an array of bytes, in big-endian format, to a HaclBignum256. */
internal fun ByteArray.toHaclBignum256(doubleMemory: Boolean = false): HaclBignum256 {
    // See detailed comments in ByteArray.toHaclBignum4096() for details on
    // what's going on here.
    val bytesToUse = when {
        size == HaclBignum256_Bytes -> this
        size == HaclBignum256_Bytes + 1 && this[0] == 0.toByte() ->
            ByteArray(HaclBignum256_Bytes) { i -> this[i+1] }
        size > HaclBignum256_Bytes ->
            throw IllegalArgumentException("ByteArray size $size is too big for $HaclBignum256_Bytes")

        else -> {
            // leading padding with zero
            val delta = HaclBignum256_Bytes - size
            ByteArray(HaclBignum256_Bytes) { i ->
                if (i < delta) 0 else this[i - delta]
            }
        }
    }
    bytesToUse.useNative { bytes ->
        val tmp: CPointer<ULongVar>? =
            Hacl_Bignum256_new_bn_from_bytes_be(HaclBignum256_Bytes.convert(), bytes)
        if (tmp == null) {
            throw OutOfMemoryError()
        }

        // make a copy to Kotlin-managed memory and free the Hacl-managed original
        val result = ULongArray((if (doubleMemory) 2 else 1) * HaclBignum256_LongWords) {
            if (it >= HaclBignum256_LongWords)
                0UL
            else
                tmp[it].convert()
        }
        free(tmp)
        return result
    }
}

/** Convert an array of bytes, in big-endian format, to a HaclBignum4096. */
internal fun ByteArray.toHaclBignum4096(doubleMemory: Boolean = false): HaclBignum4096 {
    // This code, as well as ByteArray.toHaclBignum256() is making a bunch
    // of copies. We do a first copy to get the input to exactly the right
    // length, padding or chopping zeros as necessary. Then HACL makes a
    // copy into memory that it allocated, rearranging the bytes as necessary
    // for its internal representation. Then, we make a copy of *that* into
    // memory that's managed by Kotlin, which allows the Kotlin native runtime
    // (which has a GC implementation that's evolving over time) to dispose
    // of it when it's no longer in use.

    // This might seem like it's really awful, but it's only happening when
    // we're reading this data in from an external source. What really matters
    // for performance is when we're doing arithmetic, and there we're doing
    // all the right things to avoid unnecessary copies.

    val bytesToUse = when {
        size == HaclBignum4096_Bytes -> this
        size == HaclBignum4096_Bytes + 1 && this[0] == 0.toByte() ->
            ByteArray(HaclBignum4096_Bytes) { i -> this[i+1] }
        size > HaclBignum4096_Bytes ->
            throw IllegalArgumentException("ByteArray size $size is too big for $HaclBignum4096_Bytes")

        else -> {
            // leading padding with zero
            val delta = HaclBignum4096_Bytes - size
            ByteArray(HaclBignum4096_Bytes) { i ->
                if (i < delta) 0 else this[i - delta]
            }
        }
    }
    bytesToUse.useNative { bytes ->
        val tmp: CPointer<ULongVar>? =
            Hacl_Bignum4096_new_bn_from_bytes_be(HaclBignum4096_Bytes.convert(), bytes)
        if (tmp == null) {
            throw OutOfMemoryError()
        }

        // make a copy to Kotlin-managed memory and free the Hacl-managed original
        val result = ULongArray((if (doubleMemory) 2 else 1) * HaclBignum4096_LongWords) {
            if (it >= HaclBignum4096_LongWords)
                0UL
            else
                tmp[it].convert()
        }
        free(tmp)
        return result
    }
}

/** Returns true if the given element is strictly less than the other */
internal infix fun HaclBignum256.lt256(other: HaclBignum256): Boolean {
    nativeElems(this, other) { a, b ->
        val aLtB = Hacl_Bignum256_lt_mask(a, b) != 0UL
        return aLtB
    }
}

/** Returns true if the given element is strictly greater than the other */
internal infix fun HaclBignum256.gt256(other: HaclBignum256): Boolean {
    nativeElems(this, other) { a, b ->
        val bLtA = Hacl_Bignum256_lt_mask(b, a) != 0UL
        return bLtA
    }
}

/** Returns true if the given element is strictly less than the other */
internal infix fun HaclBignum4096.lt4096(other: HaclBignum4096): Boolean {
    nativeElems(this, other) { a, b ->
        val aLtB = Hacl_Bignum4096_lt_mask(a, b) != 0UL
        return aLtB
    }
}

/** Returns true if the given element is strictly greater than the other */
internal infix fun HaclBignum4096.gt4096(other: HaclBignum4096): Boolean {
    nativeElems(this, other) { a, b ->
        val bLtA = Hacl_Bignum4096_lt_mask(b, a) != 0UL
        return bLtA
    }
}

internal fun Element.getCompat(other: GroupContext): ULongArray {
    context.assertCompatible(other)
    return when (this) {
        is ElementModP -> this.element
        is ElementModQ -> this.element
        else -> throw NotImplementedError("should only be two kinds of elements")
    }
}

actual class GroupContext(
    val pBytes: ByteArray,
    val qBytes: ByteArray,
    val gBytes: ByteArray,
    val p256minusQBytes: ByteArray,
    val rBytes: ByteArray,
    val strong: Boolean,
    val name: String,
    val powRadixOption: PowRadixOption
) {
    val p: HaclBignum4096
    val q: HaclBignum256
    val g: HaclBignum4096
    val p256minusQ: HaclBignum256
    val r: HaclBignum4096
    val zeroModP: ElementModP
    val oneModP: ElementModP
    val twoModP: ElementModP
    val gModP: ElementModP
    val gInvModP by lazy { gPowP(qMinus1ModQ) }
    val gSquaredModP by lazy { G_MOD_P * G_MOD_P }
    val qModP: ElementModP
    val zeroModQ: ElementModQ
    val oneModQ: ElementModQ
    val twoModQ: ElementModQ
    val qMinus1ModQ: ElementModQ
    val productionStrength: Boolean = strong
    val montCtxP: CPointer<Hacl_Bignum_MontArithmetic_bn_mont_ctx_u64>
    val montCtxQ: CPointer<Hacl_Bignum_MontArithmetic_bn_mont_ctx_u64>
    val dlogger: DLog

    init {
        p = pBytes.toHaclBignum4096()
        q = qBytes.toHaclBignum256()
        g = gBytes.toHaclBignum4096()
        p256minusQ = p256minusQBytes.toHaclBignum256()
        r = rBytes.toHaclBignum4096()
        zeroModP = ElementModP(0U.toHaclBignum4096(), this)
        oneModP = ElementModP(1U.toHaclBignum4096(), this)
        twoModP = ElementModP(2U.toHaclBignum4096(), this)
        gModP = ElementModP(g, this).acceleratePow()
        qModP = ElementModP(
            ULongArray(HaclBignum4096_LongWords) {
                    // Copy from 256-bit to 4096-bit, avoid problems later on. Hopefully.
                    i -> if (i >= HaclBignum256_LongWords) 0U else q[i]
            },
            this)
        zeroModQ = ElementModQ(0U.toHaclBignum4096(), this)
        oneModQ = ElementModQ(1U.toHaclBignum256(), this)
        twoModQ = ElementModQ(2U.toHaclBignum256(), this)
        qMinus1ModQ = zeroModQ - oneModQ

        // This context is something that normally needs to be freed, otherwise memory
        // leaks could occur, but we'll keep it live for the duration of the program
        // running, so we won't worry about it.
        montCtxP = p.useNative {
            Hacl_Bignum4096_mont_ctx_init(it)
                ?: throw RuntimeException("failed to make montCtxP")
        }

        montCtxQ = q.useNative {
            Hacl_Bignum256_mont_ctx_init(it)
                ?: throw RuntimeException("failed to make montCtxQ")
        }

        dlogger = DLog(this)
    }

    actual fun isProductionStrength() = productionStrength

    actual fun toJson(): JsonElement = JsonObject(mapOf()) // fixme

    override fun toString() = toJson().toString()

    actual val ZERO_MOD_P
        get() = zeroModP

    actual val ONE_MOD_P
        get() = oneModP

    actual val TWO_MOD_P
        get() = twoModP

    actual val G_MOD_P
        get() = gModP

    actual val GINV_MOD_P
        get() = gInvModP

    actual val G_SQUARED_MOD_P
        get() = gSquaredModP

    actual val Q_MOD_P
        get() = qModP

    actual val ZERO_MOD_Q
        get() = zeroModQ

    actual val ONE_MOD_Q
        get() = oneModQ

    actual val TWO_MOD_Q
        get() = twoModQ

    actual fun isCompatible(ctx: GroupContext) = this.productionStrength == ctx.productionStrength

    actual fun isCompatible(json: JsonElement): Boolean {
        throw NotImplementedError()
    }

    actual fun safeBinaryToElementModP(b: ByteArray, minimum: Int): ElementModP {
        if (minimum < 0)
            throw IllegalArgumentException("minimum $minimum may not be negative")
        else {
            // we've got an optimized path, using our Montgomery context
            val bignum4096 = b.toHaclBignum4096(doubleMemory = true)
            val result = newZeroBignum4096()
            val minimum4096 = minimum.toUInt().toHaclBignum4096()

            nativeElems(bignum4096, result, minimum4096) { s, r, m ->
                Hacl_Bignum4096_mod_precomp(montCtxP, s, r)

                // Hack to deal with the minimum part: if we're less than
                // the minimum, we'll just add the minimum back in. Since minimums
                // tend to be significantly smaller than the modulus, this won't
                // overflow. We can get away with this because the exact behavior
                // of the "safety" here is undefined, so long as the output is
                // within the expected bounds.

                if (Hacl_Bignum4096_lt_mask(r, m) != 0UL) {
                    Hacl_Bignum4096_add(r, m, r)
                }
            }
            return ElementModP(result, this)
        }
    }

    actual fun safeBinaryToElementModQ(b: ByteArray, minimum: Int): ElementModQ {
        if (minimum < 0)
           throw IllegalArgumentException("minimum $minimum may not be negative")
        else {
            val bignum256 = b.toHaclBignum256(doubleMemory = true)
            val result = newZeroBignum256()
            val minimum256 = minimum.toUInt().toHaclBignum256()

            nativeElems(bignum256, result, minimum256) { s, r, m ->
                Hacl_Bignum256_mod_precomp(montCtxQ, s, r)

                // Same hack as above.

                if (Hacl_Bignum256_lt_mask(r, m) != 0UL) {
                    Hacl_Bignum256_add(r, m, r)
                }
            }

            return ElementModQ(result, this)
        }
    }

    actual fun binaryToElementModP(b: ByteArray): ElementModP? {
        try {
            val bignum4096 = ElementModP(b.toHaclBignum4096(), this)
            if (!bignum4096.inBounds()) return null
            return bignum4096
        } catch (ex: IllegalArgumentException) {
            return null
        }
    }

    actual fun binaryToElementModQ(b: ByteArray): ElementModQ? {
        try {
            val bignum256 = ElementModQ(b.toHaclBignum256(), this)
            if (!bignum256.inBounds()) return null
            return bignum256
        } catch (ex: IllegalArgumentException) {
            return null
        }
    }

    actual fun gPowP(e: ElementModQ) = gModP powP e

    actual fun dLog(p: ElementModP): Int? = dlogger.dLog(p)
}

actual class ElementModQ(val element: HaclBignum256, val groupContext: GroupContext): Element, Comparable<ElementModQ> {
    override val context: GroupContext
    get() = groupContext

    internal fun HaclBignum256.wrap(): ElementModQ = ElementModQ(this, groupContext)

    override fun inBounds(): Boolean = element lt256 groupContext.q

    override fun isZero() = element.contentEquals(context.ZERO_MOD_Q.element)

    override fun inBoundsNoZero(): Boolean = inBounds() && !isZero()

    override fun byteArray(): ByteArray {
        val results = ByteArray(32)
        results.useNative { r ->
            element.useNative { e ->
                Hacl_Bignum256_bn_to_bytes_be(e, r)
            }
        }
        return results
    }

    actual override operator fun compareTo(other: ElementModQ): Int {
        val thisLtOther = element lt256 other.getCompat(context)

        return when {
            thisLtOther -> -1
            element.contentEquals(other.element) -> 0
            else -> 1
        }
    }

    actual operator fun plus(other: ElementModQ): ElementModQ {
        val result = newZeroBignum256()

        nativeElems(result,
            element,
            other.getCompat(groupContext),
            groupContext.q,
            groupContext.p256minusQ) { r, a, b, q, p256 ->

            val carry = Hacl_Bignum256_add(a, b, r)
            val inBoundsQ = Hacl_Bignum256_lt_mask(r, q) != 0UL
            val zeroCarry = (carry == 0UL)

            when {
                inBoundsQ && zeroCarry -> { }
                !inBoundsQ && zeroCarry -> {
                    // result - Q; which we're guaranteed is in [0,Q) because there isn't
                    // much space between Q and 2^256; this wouldn't work for the general case
                    // of arbitrary primes Q, but works for ElectionGuard because 2Q > 2^256.
//                    Hacl_Bignum256_sub(r, q, r)

                    // Cleverly, by adding the difference between q and 2^256, we'll
                    // wrap around and end up where we should have been anyway.
                    Hacl_Bignum256_add(r, p256, r)
                }
                else -> {
                    // (2^256 - Q) + result; because we overflowed; again, this only works because
                    // 2Q > 2^256, so we're just adding in the bit that was lost when we wrapped
                    // around from the original add
                    Hacl_Bignum256_add(r, p256, r)
                }
            }
        }

        return result.wrap()
    }

    actual operator fun minus(other: ElementModQ): ElementModQ {
        val result = newZeroBignum256()

        nativeElems(result,
            element,
            other.getCompat(groupContext),
            groupContext.q,
            groupContext.p256minusQ) { r, a, b, q, p256 ->

            val carry = Hacl_Bignum256_sub(a, b, r)
            val inBoundsQ = Hacl_Bignum256_lt_mask(r, q) != 0UL
            val zeroCarry = (carry == 0UL)

            if (!inBoundsQ || !zeroCarry) {
                // We underflowed, so we need to subtract the difference from the maximum
                // value (2^256) and Q. This case should be correct, regardless of whether
                // we landed in the region above Q, or anywhere else.
                Hacl_Bignum256_sub(r, p256, r)
            }
        }

        return result.wrap()
    }

    actual operator fun times(other: ElementModQ): ElementModQ {
        val result = newZeroBignum256()
        val scratch = ULongArray(HaclBignum256_LongWords * 2) // 512-bit intermediate value

        nativeElems(result, element, other.getCompat(groupContext), scratch) {
                r, a, b, s, ->
            Hacl_Bignum256_mul(a, b, s)
            Hacl_Bignum256_mod_precomp(groupContext.montCtxQ, s, r)
        }

        return result.wrap()
    }

    actual fun multInv(): ElementModQ {
        val result = newZeroBignum256()

        nativeElems(result, element) { r, e ->
            Hacl_Bignum256_mod_inv_prime_vartime_precomp(groupContext.montCtxQ, e, r)
        }

        return result.wrap()
    }

    actual operator fun unaryMinus(): ElementModQ {
        val result = newZeroBignum256()

        nativeElems(result, element, groupContext.q) { r, e, q ->
            // We're guaranteed from our type system that e is in [0,Q), so we don't
            // have to worry about underflow.
            Hacl_Bignum256_sub(q, e, r)
        }

        return result.wrap()
    }

    actual infix operator fun div(denominator: ElementModQ) = this * denominator.multInv()

    fun deepCopy() = ElementModQ(
        ULongArray(HaclBignum256_LongWords) { i -> this.element[i] },
        groupContext
    )

    override fun equals(other: Any?) = when (other) {
        // We're converting from the internal representation to a byte array
        // for equality checking; possibly overkill, but if there are ever
        // multiple internal representations, this guarantees normalization.
        is ElementModQ ->
            other.byteArray().contentEquals(this.byteArray()) &&
                    other.groupContext.isCompatible(this.groupContext)
        else -> false
    }

    override fun hashCode() = element.hashCode()

    override fun toString() = base64()  // unpleasant, but available
}

actual open class ElementModP(val element: HaclBignum4096, val groupContext: GroupContext): Element, Comparable<ElementModP> {
    override val context: GroupContext
        get() = groupContext

    internal fun HaclBignum4096.wrap(): ElementModP = ElementModP(this, groupContext)

    override fun inBounds(): Boolean = element lt4096 groupContext.p

    override fun isZero() = element.contentEquals(context.ZERO_MOD_P.element)

    override fun inBoundsNoZero() = inBounds() && !isZero()

    actual override operator fun compareTo(other: ElementModP): Int {
        val thisLtOther = element lt4096 other.getCompat(context)

        return when {
            thisLtOther -> -1
            element.contentEquals(other.element) -> 0
            else -> 1
        }
    }

    override fun byteArray(): ByteArray {
        val result = ByteArray(512)
        result.useNative { r ->
            element.useNative { e ->
                Hacl_Bignum4096_bn_to_bytes_be(e, r)
            }
        }
        return result
    }

    actual fun isValidResidue(): Boolean {
        val result = newZeroBignum4096()
        nativeElems(result, element, groupContext.q) { r, a, q ->
            Hacl_Bignum4096_mod_exp_vartime_precomp(groupContext.montCtxP, a, 256, q, r)
        }
        val residue = result.wrap() == groupContext.ONE_MOD_P
        return inBounds() && residue
    }

    actual infix open fun powP(e: ElementModQ): ElementModP {
        val result = newZeroBignum4096()
        nativeElems(result, element, e.getCompat(groupContext)) { r, a, b ->
            // We're using the faster "variable time" modular exponentiation; timing attacks
            // are not considered a significant threat against ElectionGuard.
            Hacl_Bignum4096_mod_exp_vartime_precomp(groupContext.montCtxP, a, 256, b, r)
        }
        return result.wrap()
    }

    actual operator fun times(other: ElementModP): ElementModP {
        val result = newZeroBignum4096()
        val scratch = ULongArray(HaclBignum4096_LongWords * 2)
        nativeElems(result, element, other.getCompat(groupContext), scratch) { r, a, b, s ->
            Hacl_Bignum4096_mul(a, b, s)
            Hacl_Bignum4096_mod_precomp(groupContext.montCtxP, s, r)
        }

        return result.wrap()
    }

    actual fun multInv(): ElementModP {
        // Performance note: the code below is really, really slow. Like, it's 1/17
        // the speed of the equivalent code in java.math.BigInteger. The solution
        // is that we basically never call it. Instead, throughout the code for
        // things like Chaum-Pedersen proofs, we instead raise things to powers that
        // have the side-effect of taking the inverse.

        val result = newZeroBignum4096()

        nativeElems(result, element) { r, e ->
            Hacl_Bignum4096_mod_inv_prime_vartime_precomp(groupContext.montCtxP, e, r)
        }

        return result.wrap()

        // Alternative design: taking advantage of the smaller size of the subgroup
        // reachable from the generator.

//        return this powP groupContext.qMinus1ModQ
    }

    actual infix operator fun div(denominator: ElementModP) = this * denominator.multInv()

    fun deepCopy() = ElementModP(
        ULongArray(HaclBignum4096_LongWords) { i -> this.element[i] },
        groupContext
    )

    override fun equals(other: Any?) = when (other) {
        // We're converting from the internal representation to a byte array
        // for equality checking; possibly overkill, but if there are ever
        // multiple internal representations, this guarantees normalization.
        is ElementModP ->
            other.byteArray().contentEquals(this.byteArray()) &&
                    other.groupContext.isCompatible(this.groupContext)
        else -> false
    }

    override fun hashCode() = element.hashCode()

    override fun toString() = base64()  // unpleasant, but available

    actual open fun acceleratePow() : ElementModP =
        AcceleratedElementModP(this)
}

class AcceleratedElementModP(p: ElementModP) : ElementModP(p.element, p.groupContext) {
    // Laziness to delay computation of the table until its first use; saves space
    // for PowModOptions that are never used. Also, the context isn't fully initialized
    // when constructing the GroupContext, and this avoids using it until it's ready.

    val powRadix by lazy { PowRadix(p, p.groupContext.powRadixOption) }

    override fun acceleratePow(): ElementModP = this

    override infix fun powP(e: ElementModQ) = powRadix.pow(e)
}

actual fun Iterable<ElementModQ>.addQ(): ElementModQ {
    val input = iterator().asSequence().toList()
    if (input.isEmpty()) {
        throw ArithmeticException("addQ not defined on empty lists")
    }
    if (input.count() == 1) {
        return input[0]
    }

    // There's an opportunity here to avoid creating intermediate ElementModQ instances
    // and mutate a running total instead. For now, we're just focused on correctness
    // and will circle back if/when this is performance relevant.

    return input.subList(1, input.count()).fold(input[0]) { a, b -> a + b }
}

actual fun Iterable<ElementModP>.multP(): ElementModP {
    val input = iterator().asSequence().toList()
    if (input.isEmpty()) {
        throw ArithmeticException("multP not defined on empty lists")
    }
    if (input.count() == 1) {
        return input[0]
    }

    return input.subList(1, input.count()).fold(input[0]) { a, b -> a * b }
}

actual fun UInt.toElementModQ(ctx: GroupContext) : ElementModQ = when (this) {
    0U -> ctx.ZERO_MOD_Q
    1U -> ctx.ONE_MOD_Q
    2U -> ctx.TWO_MOD_Q
    else -> ElementModQ(this.toHaclBignum256(), ctx)
}

actual fun UInt.toElementModP(ctx: GroupContext) : ElementModP = when (this) {
    0U -> ctx.ZERO_MOD_P
    1U -> ctx.ONE_MOD_P
    2U -> ctx.TWO_MOD_P
    else -> ElementModP(this.toHaclBignum4096(), ctx)
}