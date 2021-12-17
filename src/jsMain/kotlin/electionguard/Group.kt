package electionguard

import com.soywiz.krypto.encoding.fromBase64
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import electionguard.Integer as BigInteger

// If you read deep into the Git history, you'll see an older implementation here using
// kt-math (https://github.com/gciatto/kt-math), which is something
// of a port of Java's BigInteger. It's not terribly fast, but it at least seems to give
// correct answers. The code now uses GMP-WASM (https://github.com/Daninet/gmp-wasm),
// which has the potential to be a lot faster, but requires a lot of extra work to deal
// with JavaScript async issues.

internal val productionGroupContext =
    GroupContext(
        pBytes = b64ProductionP.fromBase64(),
        qBytes = b64ProductionQ.fromBase64(),
        gBytes = b64ProductionG.fromBase64(),
        rBytes = b64ProductionR.fromBase64(),
        strong = true,
        name = "production group, no acceleration"
    )

internal val testGroupContext =
    GroupContext(
        pBytes = b64TestP.fromBase64(),
        qBytes = b64TestQ.fromBase64(),
        gBytes = b64TestG.fromBase64(),
        rBytes = b64TestR.fromBase64(),
        strong = false,
        name = "16-bit test group"
    )

actual fun productionGroup(acceleration: PowRadixOption): GroupContext {
    return productionGroupContext // for now
}

actual fun testGroup() = testGroupContext

actual class GroupContext(val gmpLib: GMPLib, pBytes: ByteArray, qBytes: ByteArray, gBytes: ByteArray, rBytes: ByteArray, strong: Boolean, val name: String) {
    val p: BigInteger
    val q: BigInteger
    val g: BigInteger
    val r: BigInteger
    val zeroModP: ElementModP
    val oneModP: ElementModP
    val twoModP: ElementModP
    val gModP: ElementModP
    val gSquaredModP: ElementModP
    val qModP: ElementModP
    val zeroModQ: ElementModQ
    val oneModQ: ElementModQ
    val twoModQ: ElementModQ
    val productionStrength: Boolean = strong

    companion object {
        suspend fun make(pBytes: ByteArray, qBytes: ByteArray, gBytes: ByteArray, rBytes: ByteArray, strong: Boolean, name: String): GroupContext {
            val gmpLib = getGmpLib()
            return GroupContext(gmpLib, pBytes, qBytes, gBytes, rBytes, strong, name)
        }
    }

    init {
        gmpLib = getGmpLib()
        p = pBytes.toBigInteger(gmpLib)
        q = qBytes.toBigInteger(gmpLib)
        g = gBytes.toBigInteger(gmpLib)
        r = rBytes.toBigInteger(gmpLib)
        zeroModP = ElementModP(0U.toBigInteger(gmpLib), this)
        oneModP = ElementModP(1U.toBigInteger(gmpLib), this)
        twoModP = ElementModP(2U.toBigInteger(gmpLib), this)
        gModP = ElementModP(g, this)
        val tmp = g.timesMod(gmpLib, g, p)
        gSquaredModP = ElementModP(tmp, this)
        qModP = ElementModP(q, this)
        zeroModQ = ElementModQ(0U.toBigInteger(gmpLib), this)
        oneModQ = ElementModQ(1U.toBigInteger(gmpLib), this)
        twoModQ = ElementModQ(2U.toBigInteger(gmpLib), this)
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

        val tmp = b.toBigInteger(gmpLib).mod(gmpLib, p)
        val mv = minimum.toBigInteger(gmpLib)
        val tmp2 = if (tmp.lessThan(mv)) tmp.add(mv) else tmp
        val result = ElementModP(tmp2, this)

        return result
    }

    actual fun safeBinaryToElementModQ(b: ByteArray, minimum: Int): ElementModQ {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val tmp = b.toBigInteger(gmpLib).mod(gmpLib, p)

        val mv = minimum.toBigInteger(gmpLib)
        val tmp2 = if (tmp.lessThan(mv)) tmp.add(mv) else tmp
        val result = ElementModQ(tmp2, this)

        return result
    }


    /*
    actual fun safeBinaryToElementModP(b: ByteArray) =
        ElementModP(b.toBigInteger() % p, this)

    actual fun safeBinaryToElementModQ(b: ByteArray) =
        ElementModQ(b.toBigInteger() % q, this)
     */

    actual fun binaryToElementModP(b: ByteArray): ElementModP? {
        val tmp = b.toBigInteger(gmpLib)
        return if (tmp.greaterOrEqual(p)) null else ElementModP(tmp, this)
    }

    actual fun binaryToElementModQ(b: ByteArray): ElementModQ? {
        val tmp = b.toBigInteger(gmpLib)
        return if (tmp.greaterOrEqual(q)) null else ElementModQ(tmp, this)
    }

    actual fun gPowPSmall(e: Int) = when(e) {
        0 -> oneModP
        1 -> gModP
        2 -> gSquaredModP
        else -> gPowP(e.toElementModQ(this))
    }

    actual fun gPowP(e: ElementModQ) = gModP.powP(e)
}

internal fun Element.getCompat(other: GroupContext): BigInteger {
    context.assertCompatible(other)
    return when (this) {
        is ElementModP -> this.element
        is ElementModQ -> this.element
        else -> throw NotImplementedError("should only be two kinds of elements")
    }
}

actual class ElementModQ(val element: BigInteger, val groupContext: GroupContext): Element, Comparable<ElementModQ> {
    internal fun BigInteger.wrap(): ElementModQ = ElementModQ(this, groupContext)

    override val context: GroupContext
        get() = groupContext

    override fun inBounds() = element.greaterOrEqual(groupContext.ZERO_MOD_Q.element) && element.lessThan(groupContext.q)

    override fun inBoundsNoZero() = element.greaterThan(groupContext.ZERO_MOD_Q.element) && element.lessThan(groupContext.q)

    override fun byteArray(): ByteArray = element.toByteArray(groupContext.gmpLib)

    actual override operator fun compareTo(other: ElementModQ): Int = other.getCompat(groupContext).let {
        when {
            this.element.lessThan(it) -> -1
            this.element.greaterThan(it) -> 1
            else -> 0
        }
    }

    actual operator fun plus(other: ElementModQ) =
        this.element.plusMod(groupContext.gmpLib, other.getCompat(groupContext), groupContext.q).wrap()

    actual operator fun minus(other: ElementModQ) =
        this.element.minusMod(groupContext.gmpLib, other.getCompat(groupContext), groupContext.q).wrap()

    actual operator fun times(other: ElementModQ) =
        this.element.timesMod(groupContext.gmpLib, other.getCompat(groupContext), groupContext.q).wrap()

    actual fun multInv() = element.multiplicativeInverse(groupContext.gmpLib, groupContext.q).wrap()

    actual operator fun unaryMinus() = groupContext.q.minusMod(groupContext.gmpLib, element, groupContext.q).wrap()

    actual infix operator fun div(denominator: ElementModQ) =
        element.divMod(groupContext.gmpLib, denominator.getCompat(groupContext), groupContext.q).wrap()

    override fun equals(other: Any?) = when (other) {
        is ElementModQ -> other.element.isEqual(this.element) && other.groupContext.isCompatible(this.groupContext)
        else -> false
    }

    override fun hashCode() = element.hashCode()

    override fun toString() = element.toString(10)
}

actual class ElementModP(val element: BigInteger, val groupContext: GroupContext): Element, Comparable<ElementModP> {
    internal fun BigInteger.wrap(): ElementModP = ElementModP(this, groupContext)

    override val context: GroupContext
        get() = groupContext

    override fun inBounds() = element.greaterOrEqual(groupContext.ZERO_MOD_P.element) && element.lessThan(groupContext.p)

    override fun inBoundsNoZero() = element.greaterThan(groupContext.ZERO_MOD_P.element) && element.lessThan(groupContext.p)

    override fun byteArray(): ByteArray = element.toByteArray(groupContext.gmpLib)

    actual override operator fun compareTo(other: ElementModP): Int = other.getCompat(groupContext).let {
        when {
            this.element.lessThan(it) -> -1
            this.element.greaterThan(it) -> 1
            else -> 0
        }
    }

    actual fun isValidResidue(): Boolean {
            val residue = this.element.powMod(groupContext.gmpLib, groupContext.q, groupContext.p) == groupContext.ONE_MOD_P.element
            return inBounds() && residue
        }

    actual infix fun powP(e: ElementModQ) =
        this.element.powMod(groupContext.gmpLib, e.getCompat(groupContext), groupContext.p).wrap()

    actual operator fun times(other: ElementModP) =
        this.element.timesMod(groupContext.gmpLib, other.getCompat(groupContext), groupContext.p).wrap()

    actual fun multInv() = element.multiplicativeInverse(groupContext.gmpLib, groupContext.p).wrap()

    actual infix operator fun div(denominator: ElementModP) =
        element.divMod(groupContext.gmpLib, denominator.getCompat(groupContext), groupContext.p).wrap()

    override fun equals(other: Any?) = when (other) {
        is ElementModP -> other.element.isEqual(this.element) && other.groupContext.isCompatible(this.groupContext)
        else -> false
    }

    override fun hashCode() = element.hashCode()

    override fun toString() = element.toString(10)
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
        a.plusMod(context.gmpLib, b.getCompat(context), context.q)
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
        a.timesMod(context.gmpLib, b.getCompat(context), context.p)
    }

    return ElementModP(result, context)
}

actual fun UInt.toElementModQ(ctx: GroupContext) : ElementModQ = when (this) {
    0U -> ctx.ZERO_MOD_Q
    1U -> ctx.ONE_MOD_Q
    2U -> ctx.TWO_MOD_Q
    else -> ElementModQ(this.toBigInteger(ctx.gmpLib), ctx)
}

actual fun UInt.toElementModP(ctx: GroupContext) : ElementModP = when (this) {
    0U -> ctx.ZERO_MOD_P
    1U -> ctx.ONE_MOD_P
    2U -> ctx.TWO_MOD_P
    else -> ElementModP(this.toBigInteger(ctx.gmpLib), ctx)
}