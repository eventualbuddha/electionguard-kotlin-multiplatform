package sjcl

typealias BitArray = Array<Number>

// Apparently, the specific JavaScript that we need isn't included, by default, in
// the sjcl package. https://github.com/bitwiseshiftleft/sjcl/issues/334

fun BitArray.toByteArray(): ByteArray {
    val bl = bitArray.bitLength(this).toInt()
    var tmp: UInt = 0U
    val out = ByteArray(bl / 8) {  i ->
        if (i and 3 == 0) {
            tmp = this.get(i/4).toInt().toUInt()
        }
        val currentByte = (tmp shr 24).toUByte().toByte()
        tmp = tmp shl 8
        currentByte.toByte()
    }

    return out
}

//  fromBits: function (arr) {
//    var out = [], bl = sjcl.bitArray.bitLength(arr), i, tmp;
//    for (i=0; i<bl/8; i++) {
//      if ((i&3) === 0) {
//        tmp = arr[i/4];
//      }
//      out.push(tmp >>> 24);
//      tmp <<= 8;
//    }
//    return out;
//  },

fun ByteArray.toBitArray(): BitArray {
    val out  = MutableList<Number>(0) { 0 }
    var tmp: Int = 0
    (0..this.size - 1).forEach { i ->
        tmp = (tmp shl 8) or this[i].toInt()
        if ((i and 3) == 3) {
            out.add(tmp)
            tmp = 0
        }
    }
    if (size and 3 != 0) {
        out.add(bitArray.partial(8 * (size and 3), tmp))
    }

    // TODO: modify the logic above to directly make an Array<Number>
    //   and avoid this copying, below, since JavaScript Arrays are
    //   mutable and behave a bit like lists as well.
    return out.toTypedArray()
}

//  /** Convert from an array of bytes to a bitArray. */
//  toBits: function (bytes) {
//    var out = [], i, tmp=0;
//    for (i=0; i<bytes.length; i++) {
//      tmp = tmp << 8 | bytes[i];
//      if ((i&3) === 3) {
//        out.push(tmp);
//        tmp = 0;
//      }
//    }
//    if (i&3) {
//      out.push(sjcl.bitArray.partial(8*(i&3), tmp));
//    }
//    return out;
//  }
//};

@Suppress("NOTHING_TO_INLINE")
inline operator fun <P : SjclECCPublicKey, S : SjclECCSecretKey> SjclKeysGenerator<P, S>.invoke(curve: SjclEllipticalCurve, paranoia: Number, sec: BigNumber): SjclKeyPair<P, S> =
asDynamic()(curve, paranoia, sec)

@Suppress("NOTHING_TO_INLINE")
inline operator fun <P : SjclECCPublicKey, S : SjclECCSecretKey> SjclKeysGenerator<P, S>.invoke(curve: SjclEllipticalCurve, paranoia: Number): SjclKeyPair<P, S> =
    asDynamic()(curve, paranoia)

@Suppress("NOTHING_TO_INLINE")
inline operator fun <P : SjclECCPublicKey, S : SjclECCSecretKey> SjclKeysGenerator<P, S>.invoke(curve: Number, paranoia: Number, sec: BigNumber): SjclKeyPair<P, S> =
    asDynamic()(curve, paranoia, sec)

@Suppress("NOTHING_TO_INLINE")
inline operator fun <P : SjclECCPublicKey, S : SjclECCSecretKey> SjclKeysGenerator<P, S>.invoke(curve: Number, paranoia: Number): SjclKeyPair<P, S> =
    asDynamic()(curve, paranoia)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: SjclElGamalSecretKey, ciphertext: SjclCipherEncrypted, params: SjclCipherDecryptParams, rp: SjclCipherDecrypted): String =
    asDynamic()(password, ciphertext, params, rp)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: SjclElGamalSecretKey, ciphertext: SjclCipherEncrypted): String =
    asDynamic()(password, ciphertext)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: SjclElGamalSecretKey, ciphertext: SjclCipherEncrypted, params: SjclCipherDecryptParams): String =
    asDynamic()(password, ciphertext, params)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: SjclElGamalSecretKey, ciphertext: String, params: SjclCipherDecryptParams, rp: SjclCipherDecrypted): String =
    asDynamic()(password, ciphertext, params, rp)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: SjclElGamalSecretKey, ciphertext: String): String =
    asDynamic()(password, ciphertext)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: SjclElGamalSecretKey, ciphertext: String, params: SjclCipherDecryptParams): String =
    asDynamic()(password, ciphertext, params)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: BitArray, ciphertext: SjclCipherEncrypted, params: SjclCipherDecryptParams, rp: SjclCipherDecrypted): String =
    asDynamic()(password, ciphertext, params, rp)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: BitArray, ciphertext: SjclCipherEncrypted): String =
    asDynamic()(password, ciphertext)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: BitArray, ciphertext: SjclCipherEncrypted, params: SjclCipherDecryptParams): String =
    asDynamic()(password, ciphertext, params)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: BitArray, ciphertext: String, params: SjclCipherDecryptParams, rp: SjclCipherDecrypted): String =
    asDynamic()(password, ciphertext, params, rp)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: BitArray, ciphertext: String): String =
    asDynamic()(password, ciphertext)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: BitArray, ciphertext: String, params: SjclCipherDecryptParams): String =
    asDynamic()(password, ciphertext, params)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: String, ciphertext: SjclCipherEncrypted, params: SjclCipherDecryptParams, rp: SjclCipherDecrypted): String =
    asDynamic()(password, ciphertext, params, rp)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: String, ciphertext: SjclCipherEncrypted): String =
    asDynamic()(password, ciphertext)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: String, ciphertext: SjclCipherEncrypted, params: SjclCipherDecryptParams): String =
    asDynamic()(password, ciphertext, params)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: String, ciphertext: String, params: SjclCipherDecryptParams, rp: SjclCipherDecrypted): String =
    asDynamic()(password, ciphertext, params, rp)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: String, ciphertext: String): String =
    asDynamic()(password, ciphertext)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceDecryptor.invoke(password: String, ciphertext: String, params: SjclCipherDecryptParams): String =
    asDynamic()(password, ciphertext, params)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceEncryptor.invoke(password: SjclElGamalPublicKey, plaintext: String, params: SjclCipherEncryptParams, rp: SjclCipherEncrypted): SjclCipherEncrypted =
    asDynamic()(password, plaintext, params, rp)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceEncryptor.invoke(password: SjclElGamalPublicKey, plaintext: String): SjclCipherEncrypted =
    asDynamic()(password, plaintext)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceEncryptor.invoke(password: SjclElGamalPublicKey, plaintext: String, params: SjclCipherEncryptParams): SjclCipherEncrypted =
    asDynamic()(password, plaintext, params)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceEncryptor.invoke(password: BitArray, plaintext: String, params: SjclCipherEncryptParams, rp: SjclCipherEncrypted): SjclCipherEncrypted =
    asDynamic()(password, plaintext, params, rp)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceEncryptor.invoke(password: BitArray, plaintext: String): SjclCipherEncrypted =
    asDynamic()(password, plaintext)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceEncryptor.invoke(password: BitArray, plaintext: String, params: SjclCipherEncryptParams): SjclCipherEncrypted =
    asDynamic()(password, plaintext, params)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceEncryptor.invoke(password: String, plaintext: String, params: SjclCipherEncryptParams, rp: SjclCipherEncrypted): SjclCipherEncrypted =
    asDynamic()(password, plaintext, params, rp)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceEncryptor.invoke(password: String, plaintext: String): SjclCipherEncrypted =
    asDynamic()(password, plaintext)

@Suppress("NOTHING_TO_INLINE")
inline operator fun SjclConvenienceEncryptor.invoke(password: String, plaintext: String, params: SjclCipherEncryptParams): SjclCipherEncrypted =
    asDynamic()(password, plaintext, params)

