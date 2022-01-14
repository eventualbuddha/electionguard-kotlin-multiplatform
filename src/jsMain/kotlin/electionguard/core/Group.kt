package electionguard.core

import electionguard.core.Base64.fromSafeBase64
import electionguard.publish.Constants
import electionguard.publish.constantsFromBytes

private var globalGmpContext: GmpContext? = null

private val productionGroups =  mutableMapOf<PowRadixOption, ProductionGroupContext>()

actual suspend fun productionGroup(acceleration: PowRadixOption) : GroupContext =
    productionGroups.getOrPut(acceleration) {
        // we're only going to derive this once, then reuse it afterward
        val gmpContext: GmpContext = globalGmpContext ?: getGmpContext()
        globalGmpContext = gmpContext

        ProductionGroupContext(
            pBytes = b64ProductionP.fromSafeBase64(),
            qBytes = b64ProductionQ.fromSafeBase64(),
            gBytes = b64ProductionG.fromSafeBase64(),
            rBytes = b64ProductionR.fromSafeBase64(),
            name = "production group, ${acceleration.description}",
            powRadixOption = acceleration,
            gmpContext = gmpContext
        )
    }

class ProductionGroupContext(pBytes: ByteArray, qBytes: ByteArray, gBytes: ByteArray, rBytes: ByteArray, val name: String, val powRadixOption: PowRadixOption, val gmpContext: GmpContext) :
    GroupContext {
    val p: BigInteger
    val q: BigInteger
    val g: BigInteger
    val r: BigInteger
    val zeroModP: ProductionElementModP
    val oneModP: ProductionElementModP
    val twoModP: ProductionElementModP
    val gModP: ProductionElementModP
    val gInvModP by lazy { gPowP(qMinus1ModQ) }
    val gSquaredModP: ProductionElementModP
    val qModP: ProductionElementModP
    val qMinus1ModQ: ProductionElementModQ
    val zeroModQ: ProductionElementModQ
    val oneModQ: ProductionElementModQ
    val twoModQ: ProductionElementModQ
    val dlogger: DLog
    val big0: BigInteger
    val big1: BigInteger
    val big2: BigInteger

    init {
        console.log("ProductionGroupContext: initializing for $name")
        big0 = gmpContext.numberToBigInteger(0)
        console.log("ProductionGroupContext: big0")
        big1 = gmpContext.numberToBigInteger(1)
        console.log("ProductionGroupContext: big1")
        big2 = gmpContext.numberToBigInteger(2)
        console.log("ProductionGroupContext: big2")

        p = gmpContext.byteArrayToBigInteger(pBytes)
        console.log("ProductionGroupContext: p")
        q = gmpContext.byteArrayToBigInteger(qBytes)
        console.log("ProductionGroupContext: q")
        g = gmpContext.byteArrayToBigInteger(gBytes)
        console.log("ProductionGroupContext: g")
        r = gmpContext.byteArrayToBigInteger(rBytes)
        console.log("ProductionGroupContext: r")

        zeroModP = ProductionElementModP(big0, this)
        console.log("ProductionGroupContext: made first ProductionElement")

        oneModP = ProductionElementModP(big1, this)
        twoModP = ProductionElementModP(big2, this)
        gModP = ProductionElementModP(g, this).acceleratePow() as ProductionElementModP
        gSquaredModP = (gModP * gModP) as ProductionElementModP
        qModP = ProductionElementModP(q, this)
        zeroModQ = ProductionElementModQ(big0, this)
        oneModQ = ProductionElementModQ(big1, this)
        twoModQ = ProductionElementModQ(big2, this)
        dlogger = DLog(this)
        qMinus1ModQ = (zeroModQ - oneModQ) as ProductionElementModQ
        console.log("ProductionGroupContext: complete for $name")
    }

    override fun toString() : String = name

    override val constants: Constants by lazy {
        constantsFromBytes(pBytes, qBytes, rBytes, gBytes)
    }

    override fun isProductionStrength() = true

    override val ZERO_MOD_P: ElementModP
        get() = zeroModP

    override val ONE_MOD_P: ElementModP
        get() = oneModP

    override val TWO_MOD_P: ElementModP
        get() = twoModP

    override val G_MOD_P: ElementModP
        get() = gModP

    override val GINV_MOD_P: ElementModP
        get() = gInvModP

    override val G_SQUARED_MOD_P: ElementModP
        get() = gSquaredModP

    override val Q_MOD_P: ElementModP
        get() = qModP

    override val ZERO_MOD_Q: ElementModQ
        get() = zeroModQ

    override val ONE_MOD_Q: ElementModQ
        get() = oneModQ

    override val TWO_MOD_Q: ElementModQ
        get() = twoModQ

    override val MAX_BYTES_P: Int
        get() = 512

    override val MAX_BYTES_Q: Int
        get() = 32

    override fun isCompatible(ctx: GroupContext) = ctx.isProductionStrength()

    override fun safeBinaryToElementModP(b: ByteArray, minimum: Int): ElementModP {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val tmp = gmpContext.byteArrayToBigInteger(b) % p
        val mv = gmpContext.numberToBigInteger(minimum)
        val tmp2 = if (tmp < mv) tmp + mv else tmp
        val result = ProductionElementModP(tmp2, this)

        return result
    }

    override fun safeBinaryToElementModQ(b: ByteArray, minimum: Int, maxQMinus1: Boolean): ElementModQ {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val modulus = if (maxQMinus1) qMinus1ModQ.getCompat(this) else q

        val tmp = gmpContext.byteArrayToBigInteger(b) % modulus
        val mv = gmpContext.numberToBigInteger(minimum)

        val tmp2 = if (tmp < mv) tmp + mv else tmp
        val result = ProductionElementModQ(tmp2, this)

        return result
    }

    override fun binaryToElementModP(b: ByteArray): ElementModP? {
        val tmp = gmpContext.byteArrayToBigInteger(b)
        return if (tmp >= p || tmp < big0) null else ProductionElementModP(tmp, this)
    }

    override fun binaryToElementModQ(b: ByteArray): ElementModQ? {
        val tmp = gmpContext.byteArrayToBigInteger(b)
        return if (tmp >= q || tmp < big0) null else ProductionElementModQ(tmp, this)
    }


    override fun uIntToElementModQ(i: UInt) : ElementModQ = when (i) {
        0U -> ZERO_MOD_Q
        1U -> ONE_MOD_Q
        2U -> TWO_MOD_Q
        else -> {
            if (i <= Int.MAX_VALUE.toUInt()) {
                ProductionElementModQ(gmpContext.numberToBigInteger(i.toInt()), this)
            } else {
                // we're trying to avoid Long because in JS it's not built-in, making
                // the runtime more complicated
                throw ArithmeticException("JS uIntToElementModQ can't deal with > 2^31")
            }
        }
    }

    override fun uIntToElementModP(i: UInt) : ElementModP = when (i) {
        0U -> ZERO_MOD_P
        1U -> ONE_MOD_P
        2U -> TWO_MOD_P
        else -> {
            if (i <= Int.MAX_VALUE.toUInt()) {
                ProductionElementModP(gmpContext.numberToBigInteger(i.toInt()), this)
            } else {
                // we're trying to avoid Long because in JS it's not built-in, making
                // the runtime more complicated
                throw ArithmeticException("JS uIntToElementModP can't deal with > 2^31")
            }
        }
    }

    override fun Iterable<ElementModQ>.addQ(): ElementModQ {
        val input = iterator().asSequence().toList()

        if (input.isEmpty()) {
            throw ArithmeticException("addQ not defined on empty lists")
        }

        if (input.count() == 1) {
            return input[0]
        }

        val result = input.subList(1, input.count()).fold(input[0].getCompat(this@ProductionGroupContext)) { a, b ->
            (a + b.getCompat(this@ProductionGroupContext)).rem(this@ProductionGroupContext.q)
        }

        return ProductionElementModQ(result, this@ProductionGroupContext)
    }

    override fun Iterable<ElementModP>.multP(): ElementModP {
        val input = iterator().asSequence().toList()

        if (input.isEmpty()) {
            throw ArithmeticException("multP not defined on empty lists")
        }

        if (input.count() == 1) {
            return input[0]
        }

        val result = input.subList(1, input.count()).fold(input[0].getCompat(this@ProductionGroupContext)) { a, b ->
            (a * b.getCompat(this@ProductionGroupContext)).rem(this@ProductionGroupContext.p)
        }

        return ProductionElementModP(result, this@ProductionGroupContext)
    }

    override fun gPowP(e: ElementModQ) = gModP powP e

    override fun dLog(p: ElementModP): Int? = dlogger.dLog(p)
}

private fun Element.getCompat(other: ProductionGroupContext): BigInteger {
    context.assertCompatible(other)
    return when (this) {
        is ProductionElementModP -> this.element
        is ProductionElementModQ -> this.element
        else -> throw NotImplementedError("should only be two kinds of elements")
    }
}

class ProductionElementModQ(val element: BigInteger, val groupContext: ProductionGroupContext): ElementModQ,
    Element, Comparable<ElementModQ> {
    internal fun BigInteger.modWrap(): ElementModQ = (this % groupContext.q).wrap()
    internal fun BigInteger.wrap(): ElementModQ = ProductionElementModQ(this, groupContext)

    override val context: GroupContext
        get() = groupContext

    override fun isZero() = element == groupContext.big0

    override fun inBounds() = element >= groupContext.big0 && element < groupContext.q

    override fun inBoundsNoZero() = inBounds() && !isZero()

    override fun byteArray(): ByteArray = element.toByteArray()

    override operator fun compareTo(other: ElementModQ): Int = element.compareTo(other.getCompat(groupContext))

    override operator fun plus(other: ElementModQ) =
        (this.element + other.getCompat(groupContext)).modWrap()

    override operator fun minus(other: ElementModQ) =
        this + (-other)

    override operator fun times(other: ElementModQ) =
        (this.element * other.getCompat(groupContext)).modWrap()

    override fun multInv() = element.modInverse(groupContext.q).wrap()

    override operator fun unaryMinus(): ElementModQ =
        if (this == groupContext.zeroModQ)
            this
        else
            (groupContext.q - element).wrap()

    override infix operator fun div(denominator: ElementModQ) =
        (element * denominator.getCompat(groupContext).modInverse(groupContext.q)).modWrap()

    override infix fun powQ(e: ElementModQ) =
        this.element.modPow(e.getCompat(groupContext), groupContext.q).wrap()

    override fun equals(other: Any?) = when (other) {
        is ProductionElementModQ -> other.element == this.element && other.groupContext.isCompatible(this.groupContext)
        else -> false
    }

    override fun hashCode() = element.hashCode()

    override fun toString() = element.toString()
}

open class ProductionElementModP(val element: BigInteger, val groupContext: ProductionGroupContext): ElementModP,
    Element, Comparable<ElementModP> {
    internal fun BigInteger.modWrap(): ElementModP = (this % groupContext.p).wrap()
    internal fun BigInteger.wrap(): ElementModP = ProductionElementModP(this, groupContext)

    override val context: GroupContext
        get() = groupContext

    override fun isZero() = element == groupContext.big0

    override fun inBoundsNoZero() = inBounds() && !isZero()

    override fun inBounds() = element >= groupContext.big0 && element < groupContext.p

    override fun byteArray(): ByteArray = element.toByteArray()

    override operator fun compareTo(other: ElementModP): Int = element.compareTo(other.getCompat(groupContext))

    override fun isValidResidue(): Boolean {
        val residue = this.element.modPow(groupContext.q, groupContext.p) == groupContext.oneModP.element
        return inBounds() && residue
    }

    override infix fun powP(e: ElementModQ) =
        this.element.modPow(e.getCompat(groupContext), groupContext.p).wrap()

    override operator fun times(other: ElementModP) =
        (this.element * other.getCompat(groupContext)).modWrap()

//    override fun multInv() = element.modInverse(groupContext.p).wrap()
    override fun multInv() = this powP groupContext.qMinus1ModQ

    override infix operator fun div(denominator: ElementModP) =
        (element * denominator.getCompat(groupContext).modInverse(groupContext.p)).modWrap()

    override fun equals(other: Any?) = when (other) {
        is ProductionElementModP -> other.element == this.element && other.groupContext.isCompatible(this.groupContext)
        else -> false
    }

    override fun hashCode() = element.hashCode()

    override fun toString() = element.toString()

    override fun acceleratePow() : ElementModP =
        AcceleratedElementModP(this)
}

class AcceleratedElementModP(p: ProductionElementModP) : ProductionElementModP(p.element, p.groupContext) {
    // Laziness to delay computation of the table until its first use; saves space
    // for PowModOptions that are never used.

    val powRadix by lazy { PowRadix(p, p.groupContext.powRadixOption) }

    override fun acceleratePow(): ElementModP = this

    override infix fun powP(e: ElementModQ) = powRadix.pow(e)
}