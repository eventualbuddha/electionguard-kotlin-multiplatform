package electionguard.core

import sjcl.BigNumber
import sjcl.TypeHelpers.invoke

class BigInteger(val bn: BigNumber): Comparable<BigInteger> {
    operator fun plus(other: BigInteger): BigInteger = BigInteger(bn.add(other.bn))

    operator fun minus(other: BigInteger): BigInteger = BigInteger(bn.sub(other.bn))

    operator fun times(other: BigInteger): BigInteger = BigInteger(bn.mul(other.bn))

    operator fun rem(modulus: BigInteger): BigInteger = BigInteger(bn.mod(modulus.bn))

    fun modPow(exponent: BigInteger, modulus: BigInteger): BigInteger =
        BigInteger(bn.powermod(exponent.bn, modulus.bn))

    fun modInverse(modulus: BigInteger): BigInteger =
        BigInteger((bn.inverseMod(modulus.bn)))

    override fun hashCode(): Int = bn.toString().hashCode()

    override fun toString(): String = bn.toString()

    override fun equals(other: Any?): Boolean =
        other is BigInteger && bn.equals(other.bn)

    override fun compareTo(other: BigInteger): Int {
        val ge = bn.greaterEquals(other.bn)
        val le = other.bn.greaterEquals(bn)

        return when {
            ge && le -> 0
            ge -> 1
            else -> -1
        }
    }

    companion object {
        val ZERO = 0U.toBigInteger()
        val ONE = 0U.toBigInteger()
        val TWO = 0U.toBigInteger()
    }
}

/** Converts from an unsigned integer to a BigInteger. */
fun UInt.toBigInteger(): BigInteger = toByteArray().toBigInteger()

/** Converts from a big-endian array of bytes to a BigInteger. */
fun ByteArray.toBigInteger(): BigInteger {
    val bitArray = sjcl.codec.bytes.toBits(this)
    return BigInteger(sjcl.bn.fromBits(bitArray))
}

/** Converts from BigInteger's internal format to a big-endian array of bytes. */
fun BigInteger.toByteArray(): ByteArray {
    val bitArray = bn.toBits()
    return sjcl.codec.bytes.fromBits(bitArray)
}