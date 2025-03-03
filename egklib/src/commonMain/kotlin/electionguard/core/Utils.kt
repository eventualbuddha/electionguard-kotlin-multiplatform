package electionguard.core

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.github.michaelbull.result.*

/**
 * Our own assert function, which isn't available in the Kotlin standard library on JavaScript, even
 * though it's available on JVM and Native. If `condition` is `false`, then an `AssertionError` is
 * thrown with the given message, which defaults to "Assertion failed".
 */
fun assert(condition: Boolean, message: () -> String = { "Assertion failed" }) {
    if (!condition) {
        throw AssertionError(message())
    }
}

/**
 * Throughout our bignum arithmetic, every operation needs to check that its operands are compatible
 * (i.e., that we're not trying to use the test group and the production group interchangeably).
 * This will verify that compatibility and throw an `ArithmeticException` if they're not.
 */
fun GroupContext.assertCompatible(other: GroupContext) {
    if (!this.isCompatible(other)) {
        throw ArithmeticException("incompatible group contexts")
    }
}

/** Computes the SHA256 hash of the given string's UTF-8 representation. */
fun String.sha256(): UInt256 = encodeToByteArray().sha256()

/** Computes the HMAC-SHA256 of the given byte array using the given key. */
fun ByteArray.hmacSha256(key: UInt256): UInt256 = hmacSha256(key.bytes)

/**
 * Convert an unsigned 64-bit long into a big-endian byte array of size 1, 2, 4, or 8 bytes, as
 * necessary to fit the value.
 */
fun ULong.toByteArray(): ByteArray =
    when {
        this <= UByte.MAX_VALUE -> byteArrayOf((this and 0xffU).toByte())
        this <= UShort.MAX_VALUE ->
            byteArrayOf(((this shr 8) and 0xffU).toByte(), (this and 0xffU).toByte())
        this <= UInt.MAX_VALUE ->
            byteArrayOf(
                ((this shr 24) and 0xffU).toByte(),
                ((this shr 16) and 0xffU).toByte(),
                ((this shr 8) and 0xffU).toByte(),
                (this and 0xffU).toByte()
            )
        else ->
            byteArrayOf(
                ((this shr 56) and 0xffU).toByte(),
                ((this shr 48) and 0xffU).toByte(),
                ((this shr 40) and 0xffU).toByte(),
                ((this shr 32) and 0xffU).toByte(),
                ((this shr 24) and 0xffU).toByte(),
                ((this shr 16) and 0xffU).toByte(),
                ((this shr 8) and 0xffU).toByte(),
                (this and 0xffU).toByte()
            )
    }

/** ByteArray concatenation. */
operator fun ByteArray.plus(input2: ByteArray): ByteArray =
    ByteArray(this.size + input2.size) { i ->
        if (i < this.size) this[i] else input2[i - this.size]
    }

/** ByteArray concatenation for many individual byte arrays. Only allocates memory once. */
fun concatByteArrays(vararg bytes: ByteArray): ByteArray {
    val totalLength = bytes.sumOf { it.size }
    val result = ByteArray(totalLength)
    var offset = 0
    for (ba in bytes) {
        for (b in ba) {
            result[offset++] = b
        }
    }

    return result
}

/**
 * Convert an integer to a big-endian array of four bytes. Negative numbers will be in
 * twos-complement.
 */
fun Int.toByteArray() = this.toUInt().toByteArray()

/** Convert an integer to a big-endian array of four bytes. */
fun UInt.toByteArray() = ByteArray(4) { (this shr (24 - 8 * it) and 0xffU).toByte() }

/**
 * If there are any null values in the map, the result is null, otherwise the result is the same
 * map, but typed without the nulls.
 */
fun <K, V : Any> Map<K, V?>.noNullValuesOrNull(): Map<K, V>? {
    return if (this.any { it.value == null }) {
        null
    } else {
        @Suppress("UNCHECKED_CAST")
        this as Map<K, V>
    }
}

/**
 * If there are any null values in the list, the result is null, otherwise the result is the same
 * list, but typed without the nulls. Similar to [requireNoNulls], but returns `null` rather than
 * throwing an exception.
 */
fun <T : Any> List<T?>.noNullValuesOrNull(): List<T>? {
    return if (this.any { it == null }) {
        null
    } else {
        @Suppress("UNCHECKED_CAST")
        this as List<T>
    }
}

/**
 * Normally, Kotlin's `Enum.valueOf` or [enumValueOf] method will throw an exception for an invalid
 * input. This method will instead return `null` if the string doesn't map to a valid value of the
 * enum.
 */
inline fun <reified T : Enum<T>> safeEnumValueOf(name: String?): T? {
    if (name == null) {
        return null
    }

    return try {
        enumValueOf<T>(name)
    } catch (e: IllegalArgumentException) {
        null
    }
}

fun getSystemDate(): LocalDateTime {
    val currentMoment: Instant = Clock.System.now()
    return currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
}

/**
 * Helper function: given a list of [Result] instances with string errors,
 * either merges together all the strings (with an optional header provided
 * by a lambda), or returns an [Ok] with the value provided by another lambda.
 */
inline fun <T> List<Result<T, String>>.mergeWithOkay(
    errHeaderProvider: () -> String = { "" },
    okProvider: () -> T) : Result<T, String> {
    val errors =  filterIsInstance<Err<String>>()

    return if (errors.isEmpty()) {
        Ok(okProvider())
    } else {
        val errHeader = errHeaderProvider()
        val errHeaderWithNewline =
            if (errHeader.isNotBlank())
                errHeader + "\n"
            else
                ""
        Err(errHeaderWithNewline +
                errors.joinToString("\n ") { it.error })
    }
}

/**
 * Helper function: given a list of [Result] instances, of the type we often use in
 * ElectionGuard, merges together all the results. If they're all Ok, the result is
 * Ok. If any are Err, then the result is an Err with all the strings joined by newlines.
 */
fun List<Result<Boolean, String>>.merge(): Result<Boolean, String> =
    mergeWithOkay { true }

// Note: making these merge() functions inline will have a non-trivial
// performance benefit on the common case that there are no errors.
// In particular, the call to okProvider() will be inlined, which
// will eliminate any method call (except isEmpty()) for the common
// case of returning a constant, like Ok(true).

