package electionguard

import electionguard.Base64.fromSafeBase64
import electionguard.Base64.toBase64
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import sjcl.BigNumber
import sjcl.TypeHelpers.invoke
import sjcl.bn
import sjcl.toBitArray
import sjcl.toByteArray

// This implementation uses the Stanford JavaScript Crypto Library
// (https://github.com/bitwiseshiftleft/sjcl), which is relatively widely used.
// This might need to be replaced with something that will be performant,
// probably using WASM. The "obvious" choices are:
//
// - GMP-WASM (https://github.com/Daninet/gmp-wasm)
//   (Kotlin's "Dukat" TypeScript interface extraction completely fails on this, which is sad.)
//
// - HACL-WASM (https://github.com/project-everest/hacl-star/tree/master/bindings/js#readme)
//   (Hash many other HACL features, but doesn't expose any of the BigInt-related types.)
//
// But, for now, JS will at least "work".

internal operator fun BigNumber.plus(b: BigNumber) = this.add(b)

internal operator fun BigNumber.minus(b: BigNumber) = this.sub(b)

internal operator fun BigNumber.times(b: BigNumber) = this.mul(b)

internal fun timesMod(a: BigNumber, b: BigNumber, n: BigNumber) = a.mulmod(b, n)

internal operator fun BigNumber.compareTo(b: BigNumber) = when {
    this.equals(b) -> 0
    this.greaterEquals(b) -> 1
    else -> -1
}

internal fun BigNumber.modPow(e: BigNumber, n: BigNumber) = this.powermod(e, n)

internal fun BigNumber.modInverse(n: BigNumber) = this.inverseMod(n)

internal fun BigNumber.toByteArray(): ByteArray = this.toBits().toByteArray()

private val testGroupContext =
    GroupContext(
        pBytes = b64TestP.fromSafeBase64(),
        qBytes = b64TestQ.fromSafeBase64(),
        gBytes = b64TestG.fromSafeBase64(),
        rBytes = b64TestR.fromSafeBase64(),
        strong = false,
        name = "16-bit test group",
        powRadixOption = PowRadixOption.NO_ACCELERATION
    )

private val productionGroups: HashMap<PowRadixOption, GroupContext> =
    hashMapOf(
        PowRadixOption.NO_ACCELERATION to
                GroupContext(
                    pBytes = b64ProductionP.fromSafeBase64(),
                    qBytes = b64ProductionQ.fromSafeBase64(),
                    gBytes = b64ProductionG.fromSafeBase64(),
                    rBytes = b64ProductionR.fromSafeBase64(),
                    strong = true,
                    name = "production group, no acceleration",
                    powRadixOption = PowRadixOption.NO_ACCELERATION
                ),
        PowRadixOption.LOW_MEMORY_USE to
                GroupContext(
                    pBytes = b64ProductionP.fromSafeBase64(),
                    qBytes = b64ProductionQ.fromSafeBase64(),
                    gBytes = b64ProductionG.fromSafeBase64(),
                    rBytes = b64ProductionR.fromSafeBase64(),
                    strong = true,
                    name = "production group, low memory use",
                    powRadixOption = PowRadixOption.LOW_MEMORY_USE
                ),
        PowRadixOption.HIGH_MEMORY_USE to
                GroupContext(
                    pBytes = b64ProductionP.fromSafeBase64(),
                    qBytes = b64ProductionQ.fromSafeBase64(),
                    gBytes = b64ProductionG.fromSafeBase64(),
                    rBytes = b64ProductionR.fromSafeBase64(),
                    strong = true,
                    name = "production group, high memory use",
                    powRadixOption = PowRadixOption.HIGH_MEMORY_USE
                ),
        PowRadixOption.EXTREME_MEMORY_USE to
                GroupContext(
                    pBytes = b64ProductionP.fromSafeBase64(),
                    qBytes = b64ProductionQ.fromSafeBase64(),
                    gBytes = b64ProductionG.fromSafeBase64(),
                    rBytes = b64ProductionR.fromSafeBase64(),
                    strong = true,
                    name = "production group, extreme memory use",
                    powRadixOption = PowRadixOption.EXTREME_MEMORY_USE
                )
    )

actual fun productionGroup(acceleration: PowRadixOption) : GroupContext =
    productionGroups[acceleration] ?: throw Error("can't happen")

actual fun testGroup() = testGroupContext

/** Convert an array of bytes, in big-endian format, to a BigInteger */
internal fun UInt.toBigNumber(): BigNumber {
    val bytes = ByteArray(4)
    // big-endian
    bytes[0] = ((this and 0xff_00_00_00U) shr 24).toByte()
    bytes[1] = ((this and 0xff_00_00U) shr 16).toByte()
    bytes[2] = ((this and 0xff_00U) shr 8).toByte()
    bytes[3] = ((this and 0xffU)).toByte()
    return bytes.toBigNumber()
}

internal fun ByteArray.toBigNumber(): BigNumber = bn.fromBits(this.toBitArray())

private val SJCL_ZERO = 0U.toBigNumber()
private val SJCL_ONE = 1U.toBigNumber()
private val SJCL_TWO = 2U.toBigNumber()

actual class GroupContext(pBytes: ByteArray, qBytes: ByteArray, gBytes: ByteArray, rBytes: ByteArray, strong: Boolean, val name: String, val powRadixOption: PowRadixOption) {
    val p: BigNumber
    val q: BigNumber
    val g: BigNumber
    val r: BigNumber
    val zeroModP: ElementModP
    val oneModP: ElementModP
    val twoModP: ElementModP
    val gModP: ElementModP
    val gInvModP by lazy { gPowP(qMinus1ModQ) }
    val gSquaredModP: ElementModP
    val qModP: ElementModP
    val qMinus1ModQ: ElementModQ
    val pMinus1ModP: ElementModP
    val zeroModQ: ElementModQ
    val oneModQ: ElementModQ
    val twoModQ: ElementModQ
    val productionStrength: Boolean = strong
    val dlogger: DLog

    init {
        p = pBytes.toBigNumber()
        q = qBytes.toBigNumber()
        g = gBytes.toBigNumber()
        r = rBytes.toBigNumber()
        zeroModP = ElementModP(SJCL_ZERO, this)
        oneModP = ElementModP(SJCL_ONE, this)
        twoModP = ElementModP(SJCL_TWO, this)
        println("ZeroP = $zeroModP")
        println("OneP = $oneModP")
        println("TwoP = $twoModP")
        gModP = ElementModP(g, this).acceleratePow()
        gSquaredModP = gModP * gModP
        qModP = ElementModP(q, this)
        zeroModQ = ElementModQ(SJCL_ZERO, this)
        oneModQ = ElementModQ(SJCL_ONE, this)
        twoModQ = ElementModQ(SJCL_TWO, this)
        dlogger = DLog(this)
        qMinus1ModQ = zeroModQ - oneModQ
        pMinus1ModP = ElementModP(p - SJCL_ONE, this)
    }

    actual fun toJson(): JsonElement = JsonObject(mapOf()) // fixme

    actual fun isProductionStrength() = productionStrength

    actual val ZERO_MOD_P: ElementModP
        get() = zeroModP

    actual val ONE_MOD_P: ElementModP
        get() = oneModP

    actual val TWO_MOD_P: ElementModP
        get() = twoModP

    actual val G_MOD_P: ElementModP
        get() = gModP

    actual val GINV_MOD_P: ElementModP
        get() = gInvModP

    actual val G_SQUARED_MOD_P: ElementModP
        get() = gSquaredModP

    actual val Q_MOD_P: ElementModP
        get() = qModP

    actual val ZERO_MOD_Q: ElementModQ
        get() = zeroModQ

    actual val ONE_MOD_Q: ElementModQ
        get() = oneModQ

    actual val TWO_MOD_Q: ElementModQ
        get() = twoModQ

    actual fun isCompatible(ctx: GroupContext) = productionStrength == ctx.productionStrength

    actual fun isCompatible(json: JsonElement): Boolean {
        TODO("Not yet implemented")
    }

    actual fun safeBinaryToElementModP(b: ByteArray, minimum: Int): ElementModP {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val tmp = b.toBigNumber().mod(p)
        val mv = minimum.toUInt().toBigNumber()
        val tmp2 = if (tmp < mv) tmp + mv else tmp
        val result = ElementModP(tmp2, this)

        return result
    }

    actual fun safeBinaryToElementModQ(b: ByteArray, minimum: Int): ElementModQ {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val tmp = b.toBigNumber().mod(q)

        val mv = minimum.toUInt().toBigNumber()
        val tmp2 = if (tmp < mv) tmp + mv else tmp
        val result = ElementModQ(tmp2, this)

        return result
    }

    actual fun binaryToElementModP(b: ByteArray): ElementModP? {
        val tmp = b.toBigNumber()
        return if (tmp >= p || tmp < SJCL_ZERO) null else ElementModP(tmp, this)
    }

    actual fun binaryToElementModQ(b: ByteArray): ElementModQ? {
        val tmp = b.toBigNumber()
        return if (tmp >= q || tmp < SJCL_ZERO) null else ElementModQ(tmp, this)
    }

    actual fun gPowP(e: ElementModQ) = gModP powP e

    actual fun dLog(p: ElementModP): Int? = dlogger.dLog(p)
}

internal fun Element.getCompat(other: GroupContext): BigNumber {
    context.assertCompatible(other)
    return when (this) {
        is ElementModP -> this.element
        is ElementModQ -> this.element
        else -> throw NotImplementedError("should only be two kinds of elements")
    }
}

actual class ElementModQ(val element: BigNumber, val groupContext: GroupContext): Element, Comparable<ElementModQ> {
    internal fun BigNumber.modWrap(): ElementModQ = this.mod(groupContext.q).wrap()
    internal fun BigNumber.wrap(): ElementModQ = ElementModQ(this, groupContext)

    override val context: GroupContext
        get() = groupContext

    override fun isZero() = element == groupContext.ZERO_MOD_Q.element

    override fun inBounds() = element >= groupContext.ZERO_MOD_Q.element && element < groupContext.q

    override fun inBoundsNoZero() = inBounds() && !isZero()

    override fun byteArray(): ByteArray = element.toByteArray()

    actual override operator fun compareTo(other: ElementModQ) = element.compareTo(other.getCompat(context))

    actual operator fun plus(other: ElementModQ) =
        (this.element + other.getCompat(groupContext)).modWrap()

    actual operator fun minus(other: ElementModQ) =
        (this.element - other.getCompat(groupContext)).modWrap()

    actual operator fun times(other: ElementModQ): ElementModQ =
        timesMod(this.element, other.getCompat(groupContext), groupContext.q).wrap()

    actual fun multInv(): ElementModQ = element.modInverse(groupContext.q).wrap()

    actual operator fun unaryMinus(): ElementModQ = (groupContext.q - element).wrap()

    actual infix operator fun div(denominator: ElementModQ) =
        (element * denominator.getCompat(groupContext).modInverse(groupContext.q)).modWrap()

    override fun equals(other: Any?) = when (other) {
        is ElementModQ -> other.getCompat(this.groupContext).equals(this.element)
        else -> false
    }

    override fun hashCode() = element.toByteArray().hashCode()

    override fun toString() = element.toByteArray().toBase64()
}

actual open class ElementModP(val element: BigNumber, val groupContext: GroupContext): Element, Comparable<ElementModP> {
    internal fun BigNumber.modWrap(): ElementModP = this.mod(groupContext.p).wrap()
    internal fun BigNumber.wrap(): ElementModP = ElementModP(this, groupContext)

    override val context: GroupContext
        get() = groupContext

    override fun isZero() = element.equals(SJCL_ZERO)

    override fun inBoundsNoZero() = inBounds() && !isZero()

    override fun inBounds() = element >= groupContext.ZERO_MOD_P.element && element < groupContext.p

    override fun byteArray(): ByteArray = element.toByteArray()

    actual override operator fun compareTo(other: ElementModP) = element.compareTo(other.getCompat(context))

    actual fun isValidResidue(): Boolean {
        val residue = element.modPow(groupContext.q, groupContext.p) == groupContext.ONE_MOD_P.element
        return inBounds() && residue
    }

    actual infix open fun powP(e: ElementModQ) =
        this.element.modPow(e.getCompat(groupContext), groupContext.p).wrap()

    actual operator fun times(other: ElementModP) =
        timesMod(this.element, other.getCompat(groupContext), groupContext.p).wrap()

    actual fun multInv() = element.modInverse(groupContext.p).wrap()

    actual infix operator fun div(denominator: ElementModP) =
        (element * denominator.getCompat(groupContext).modInverse(groupContext.p)).modWrap()

    override fun equals(other: Any?) = when (other) {
        is ElementModP -> other.getCompat(this.groupContext).equals(this.element)
        else -> false
    }

    override fun hashCode() = element.toByteArray().hashCode()

    override fun toString() = element.toByteArray().toBase64()

    actual open fun acceleratePow() : ElementModP =
        AcceleratedElementModP(this)
}

class AcceleratedElementModP(p: ElementModP) : ElementModP(p.element, p.groupContext) {
    // Laziness to delay computation of the table until its first use; saves space
    // for PowModOptions that are never used.

    val powRadix by lazy { PowRadix(p, p.groupContext.powRadixOption) }

    override fun acceleratePow(): ElementModP = this

    override infix fun powP(e: ElementModQ) = powRadix.pow(e)
}

actual fun Iterable<ElementModQ>.addQ(): ElementModQ {
    val input = iterator().asSequence().toList()
    if (input.isEmpty()) {
        throw ArithmeticException("addQ not defined on empty lists")
    }

    // We're going to mutate the state of the result; it starts with the
    // first entry in the list, and then we add each subsequent entry.
    val context = input[0].groupContext

    val result = input.subList(1, input.count()).fold(input[0].element) { a, b ->
        (a + b.getCompat(context)).mod(context.q)
    }

    return ElementModQ(result, context)
}

actual fun Iterable<ElementModP>.multP(): ElementModP {
    val input = iterator().asSequence().toList()
    if (input.isEmpty()) {
        throw ArithmeticException("multP not defined on empty lists")
    }
    if (input.count() == 1) {
        return input[0]
    }

    // We're going to mutate the state of the result; it starts with the
    // first entry in the list, and then we add each subsequent entry.
    val context = input[0].groupContext

    val result = input.subList(1, input.count()).fold(input[0].element) { a, b ->
        (a * b.getCompat(context)).mod(context.p)
    }

    return ElementModP(result, context)
}

actual fun UInt.toElementModQ(ctx: GroupContext) : ElementModQ = when (this) {
    0U -> ctx.ZERO_MOD_Q
    1U -> ctx.ONE_MOD_Q
    2U -> ctx.TWO_MOD_Q
    else -> ElementModQ(this.toBigNumber(), ctx)
}

actual fun UInt.toElementModP(ctx: GroupContext) : ElementModP = when (this) {
    0U -> ctx.ZERO_MOD_P
    1U -> ctx.ONE_MOD_P
    2U -> ctx.TWO_MOD_P
    else -> ElementModP(this.toBigNumber(), ctx)
}