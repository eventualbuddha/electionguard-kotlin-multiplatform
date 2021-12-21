@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
@file:JsModule("sjcl-complete")
package sjcl

import org.khronos.webgl.ArrayBuffer
import sjcl.TypeHelpers.BigNumberBinaryOperator
import sjcl.TypeHelpers.BigNumberTrinaryOperator
import sjcl.TypeHelpers.Bind1

external var arrayBuffer: SjclArrayBufferModes

external var bn: BigNumberStatic

external var bitArray: BitArrayStatic

external var codec: SjclCodecs

external var hash: SjclHashes

external var exception: SjclExceptions

external var cipher: SjclCiphers

external var mode: SjclModes

external var misc: SjclMisc

external var ecc: SjclEllipticCurveCryptography

external var random: SjclRandom

external var prng: SjclRandomStatic

external var keyexchange: SjclKeyExchange

external var json: SjclJson

external var encrypt: SjclConvenienceEncryptor

external var decrypt: SjclConvenienceDecryptor

external interface BigNumber {
    var radix: Number
    var maxMul: Number
    fun copy(): BigNumber
    var initWith: BigNumberBinaryOperator
    fun equals(that: BigNumber): Boolean
    fun equals(that: Number): Boolean
    fun getLimb(index: Number): Number
    fun greaterEquals(that: BigNumber): Boolean
    fun greaterEquals(that: Number): Boolean
    override fun toString(): String
    var addM: BigNumberBinaryOperator
    fun doubleM(): BigNumber
    fun halveM(): BigNumber
    var subM: BigNumberBinaryOperator
    var mod: BigNumberBinaryOperator
    var inverseMod: BigNumberBinaryOperator
    var add: BigNumberBinaryOperator
    var sub: BigNumberBinaryOperator
    var mul: BigNumberBinaryOperator
    fun square(): BigNumber
    fun power(n: BigNumber): BigNumber
    fun power(n: Array<Number>): BigNumber
    fun power(n: Number): BigNumber
    var mulmod: BigNumberTrinaryOperator
    var powermod: BigNumberTrinaryOperator
    var montpowermod: BigNumberTrinaryOperator
    fun trim(): BigNumber
    fun reduce(): BigNumber
    fun fullReduce(): BigNumber
    fun normalize(): BigNumber
    fun cnormalize(): BigNumber
    fun toBits(len: Number = definedExternally): BitArray
    fun bitLength(): Number
}

external interface `T$0` {
    var p127: PseudoMersennePrimeStatic
    var p25519: PseudoMersennePrimeStatic
    var p192k: PseudoMersennePrimeStatic
    var p224k: PseudoMersennePrimeStatic
    var p256k: PseudoMersennePrimeStatic
    var p192: PseudoMersennePrimeStatic
    var p224: PseudoMersennePrimeStatic
    var p256: PseudoMersennePrimeStatic
    var p384: PseudoMersennePrimeStatic
    var p521: PseudoMersennePrimeStatic
}

external interface BigNumberStatic {
    fun fromBits(bits: BitArray): BigNumber
    var random: Bind1<Number>
    var prime: `T$0`
    fun pseudoMersennePrime(exponent: Number, coeff: Array<Array<Number>>): PseudoMersennePrimeStatic
}

external interface PseudoMersennePrime : BigNumber {
    override fun reduce(): PseudoMersennePrime
    override fun fullReduce(): PseudoMersennePrime
    fun inverse(): PseudoMersennePrime
}

external interface PseudoMersennePrimeStatic : BigNumberStatic

external interface BitArrayStatic {
    fun bitSlice(a: BitArray, bstart: Number, bend: Number): BitArray
    fun extract(a: BitArray, bstart: Number, blength: Number): Number
    fun concat(a1: BitArray, a2: BitArray): BitArray
    fun bitLength(a: BitArray): Number
    fun clamp(a: BitArray, len: Number): BitArray
    fun partial(len: Number, x: Number, _end: Number = definedExternally): Number
    fun getPartial(x: Number): Number
    fun equal(a: BitArray, b: BitArray): Boolean
    fun _shiftRight(a: BitArray, shift: Number, carry: Number = definedExternally, out: BitArray = definedExternally): BitArray
    fun _xor4(x: Array<Number>, y: Array<Number>): Array<Number>
    fun byteswapM(a: BitArray): BitArray
}

external interface SjclCodec<T> {
    fun fromBits(bits: BitArray): T
    fun toBits(value: T): BitArray
}

external interface SjclArrayBufferCodec : SjclCodec<ArrayBuffer> {
    fun fromBits(bits: BitArray, padding: Boolean = definedExternally, padding_count: Number = definedExternally): ArrayBuffer
    fun hexDumpBuffer(buffer: ArrayBuffer)
}

external interface SjclCodecs {
    var arrayBuffer: SjclArrayBufferCodec
    var utf8String: SjclCodec<String>
    var hex: SjclCodec<String>
    var bytes: SjclCodec<Array<Number>>
    var base32: SjclCodec<String>
    var base32hex: SjclCodec<String>
    var base64: SjclCodec<String>
    var base64url: SjclCodec<String>
    var z85: SjclCodec<String>
}

external interface SjclHash {
    fun reset(): SjclHash
    fun update(data: BitArray): SjclHash
    fun update(data: String): SjclHash
    fun finalize(): BitArray
}

external interface SjclHashStatic {
    fun hash(data: BitArray): BitArray
    fun hash(data: String): BitArray
}

external interface SjclHashes {
    var sha1: SjclHashStatic
    var sha256: SjclHashStatic
    var sha512: SjclHashStatic
    var ripemd160: SjclHashStatic
}

external interface SjclExceptions {
    var corrupt: SjclExceptionFactory
    var invalid: SjclExceptionFactory
    var bug: SjclExceptionFactory
    var notReady: SjclExceptionFactory
}

external interface SjclExceptionFactory

external interface SjclCiphers {
    var aes: SjclCipherStatic
}

external interface SjclCipher {
    fun encrypt(data: Array<Number>): Array<Number>
    fun decrypt(data: Array<Number>): Array<Number>
}

external interface SjclCipherStatic

external interface SjclModes {
    var gcm: SjclGCMMode
    var ccm: SjclCCMMode
    var ocb2: SjclOCB2Mode
    var ocb2progressive: SjclOCB2ProgressiveMode
    var cbc: SjclCBCMode
    var ctr: SjclCTRMode
}

external interface SjclArrayBufferModes {
    var ccm: SjclArrayBufferCCMMode
}

external interface SjclGCMMode {
    fun encrypt(prf: SjclCipher, plaintext: BitArray, iv: BitArray, adata: BitArray = definedExternally, tlen: Number = definedExternally): BitArray
    fun decrypt(prf: SjclCipher, ciphertext: BitArray, iv: BitArray, adata: BitArray = definedExternally, tlen: Number = definedExternally): BitArray
}

external interface SjclArrayBufferCCMMode {
    fun compat_encrypt(prf: SjclCipher, plaintext: BitArray, iv: BitArray, adata: BitArray = definedExternally, tlen: Number = definedExternally): BitArray
    fun compat_decrypt(prf: SjclCipher, ciphertext: BitArray, iv: BitArray, adata: BitArray = definedExternally, tlen: Number = definedExternally): BitArray
    fun encrypt(prf: SjclCipher, plaintext_buffer: ArrayBuffer, iv: BitArray, adata: ArrayBuffer = definedExternally, tlen: Number = definedExternally, ol: Number = definedExternally): ArrayBuffer
    fun decrypt(prf: SjclCipher, ciphertext_buffer: ArrayBuffer, iv: BitArray, tag: BitArray, adata: ArrayBuffer = definedExternally, tlen: Number = definedExternally, ol: Number = definedExternally): ArrayBuffer
}

external interface SjclCCMMode {
    fun listenProgress(cb: (param_val: Number) -> Unit)
    fun unListenProgress(cb: (param_val: Number) -> Unit)
    fun encrypt(prf: SjclCipher, plaintext: BitArray, iv: BitArray, adata: BitArray = definedExternally, tlen: Number = definedExternally): BitArray
    fun decrypt(prf: SjclCipher, ciphertext: BitArray, iv: BitArray, adata: BitArray = definedExternally, tlen: Number = definedExternally): BitArray
}

external interface SjclOCB2Mode {
    fun encrypt(prf: SjclCipher, plaintext: BitArray, iv: BitArray, adata: BitArray = definedExternally, tlen: Number = definedExternally, premac: Boolean = definedExternally): BitArray
    fun decrypt(prf: SjclCipher, ciphertext: BitArray, iv: BitArray, adata: BitArray = definedExternally, tlen: Number = definedExternally, premac: Boolean = definedExternally): BitArray
    fun pmac(prf: SjclCipher, adata: BitArray): Array<Number>
}

external interface SjclOCB2ProgressiveProcessor {
    fun process(data: BitArray): BitArray
    fun finalize(): BitArray
}

external interface SjclOCB2ProgressiveMode {
    fun createEncryptor(prp: SjclCipher, iv: BitArray, adata: BitArray = definedExternally, tlen: Number = definedExternally, premac: Boolean = definedExternally): SjclOCB2ProgressiveProcessor
    fun createDecryptor(prp: SjclCipher, iv: BitArray, adata: BitArray = definedExternally, tlen: Number = definedExternally, premac: Boolean = definedExternally): SjclOCB2ProgressiveProcessor
}

external interface SjclCBCMode {
    fun encrypt(prf: SjclCipher, plaintext: BitArray, iv: BitArray, adata: BitArray = definedExternally): BitArray
    fun decrypt(prf: SjclCipher, ciphertext: BitArray, iv: BitArray, adata: BitArray = definedExternally): BitArray
}

external interface SjclCTRMode {
    fun encrypt(prf: SjclCipher, plaintext: BitArray, iv: BitArray, adata: BitArray = definedExternally): BitArray
    fun decrypt(prf: SjclCipher, ciphertext: BitArray, iv: BitArray, adata: BitArray = definedExternally): BitArray
}

external interface PBKDF2Params {
    var iter: Number?
        get() = definedExternally
        set(value) = definedExternally
    var salt: BitArray?
        get() = definedExternally
        set(value) = definedExternally
}

external interface `T$1` {
    var key: BitArray
    var salt: BitArray
}

external interface SjclMisc {
    fun pbkdf2(password: BitArray, salt: BitArray, count: Number = definedExternally, length: Number = definedExternally, Prff: SjclPRFFamilyStatic = definedExternally): BitArray
    fun pbkdf2(password: BitArray, salt: BitArray): BitArray
    fun pbkdf2(password: BitArray, salt: BitArray, count: Number = definedExternally): BitArray
    fun pbkdf2(password: BitArray, salt: BitArray, count: Number = definedExternally, length: Number = definedExternally): BitArray
    fun pbkdf2(password: BitArray, salt: String, count: Number = definedExternally, length: Number = definedExternally, Prff: SjclPRFFamilyStatic = definedExternally): BitArray
    fun pbkdf2(password: BitArray, salt: String): BitArray
    fun pbkdf2(password: BitArray, salt: String, count: Number = definedExternally): BitArray
    fun pbkdf2(password: BitArray, salt: String, count: Number = definedExternally, length: Number = definedExternally): BitArray
    fun pbkdf2(password: String, salt: BitArray, count: Number = definedExternally, length: Number = definedExternally, Prff: SjclPRFFamilyStatic = definedExternally): BitArray
    fun pbkdf2(password: String, salt: BitArray): BitArray
    fun pbkdf2(password: String, salt: BitArray, count: Number = definedExternally): BitArray
    fun pbkdf2(password: String, salt: BitArray, count: Number = definedExternally, length: Number = definedExternally): BitArray
    fun pbkdf2(password: String, salt: String, count: Number = definedExternally, length: Number = definedExternally, Prff: SjclPRFFamilyStatic = definedExternally): BitArray
    fun pbkdf2(password: String, salt: String): BitArray
    fun pbkdf2(password: String, salt: String, count: Number = definedExternally): BitArray
    fun pbkdf2(password: String, salt: String, count: Number = definedExternally, length: Number = definedExternally): BitArray
    var hmac: SjclHMACStatic
    fun cachedPbkdf2(password: String, obj: PBKDF2Params = definedExternally): `T$1`
    fun hkdf(ikm: BitArray, keyBitLength: Number, salt: BitArray, info: BitArray, Hash: SjclHashStatic = definedExternally): BitArray
    fun hkdf(ikm: BitArray, keyBitLength: Number, salt: BitArray, info: BitArray): BitArray
    fun hkdf(ikm: BitArray, keyBitLength: Number, salt: BitArray, info: String, Hash: SjclHashStatic = definedExternally): BitArray
    fun hkdf(ikm: BitArray, keyBitLength: Number, salt: BitArray, info: String): BitArray
    fun hkdf(ikm: BitArray, keyBitLength: Number, salt: String, info: BitArray, Hash: SjclHashStatic = definedExternally): BitArray
    fun hkdf(ikm: BitArray, keyBitLength: Number, salt: String, info: BitArray): BitArray
    fun hkdf(ikm: BitArray, keyBitLength: Number, salt: String, info: String, Hash: SjclHashStatic = definedExternally): BitArray
    fun hkdf(ikm: BitArray, keyBitLength: Number, salt: String, info: String): BitArray
    fun scrypt(password: BitArray, salt: BitArray, N: Number = definedExternally, r: Number = definedExternally, p: Number = definedExternally, length: Number = definedExternally, Prff: SjclPRFFamilyStatic = definedExternally): BitArray
    fun scrypt(password: BitArray, salt: BitArray): BitArray
    fun scrypt(password: BitArray, salt: BitArray, N: Number = definedExternally): BitArray
    fun scrypt(password: BitArray, salt: BitArray, N: Number = definedExternally, r: Number = definedExternally): BitArray
    fun scrypt(password: BitArray, salt: BitArray, N: Number = definedExternally, r: Number = definedExternally, p: Number = definedExternally): BitArray
    fun scrypt(password: BitArray, salt: BitArray, N: Number = definedExternally, r: Number = definedExternally, p: Number = definedExternally, length: Number = definedExternally): BitArray
    fun scrypt(password: BitArray, salt: String, N: Number = definedExternally, r: Number = definedExternally, p: Number = definedExternally, length: Number = definedExternally, Prff: SjclPRFFamilyStatic = definedExternally): BitArray
    fun scrypt(password: BitArray, salt: String): BitArray
    fun scrypt(password: BitArray, salt: String, N: Number = definedExternally): BitArray
    fun scrypt(password: BitArray, salt: String, N: Number = definedExternally, r: Number = definedExternally): BitArray
    fun scrypt(password: BitArray, salt: String, N: Number = definedExternally, r: Number = definedExternally, p: Number = definedExternally): BitArray
    fun scrypt(password: BitArray, salt: String, N: Number = definedExternally, r: Number = definedExternally, p: Number = definedExternally, length: Number = definedExternally): BitArray
    fun scrypt(password: String, salt: BitArray, N: Number = definedExternally, r: Number = definedExternally, p: Number = definedExternally, length: Number = definedExternally, Prff: SjclPRFFamilyStatic = definedExternally): BitArray
    fun scrypt(password: String, salt: BitArray): BitArray
    fun scrypt(password: String, salt: BitArray, N: Number = definedExternally): BitArray
    fun scrypt(password: String, salt: BitArray, N: Number = definedExternally, r: Number = definedExternally): BitArray
    fun scrypt(password: String, salt: BitArray, N: Number = definedExternally, r: Number = definedExternally, p: Number = definedExternally): BitArray
    fun scrypt(password: String, salt: BitArray, N: Number = definedExternally, r: Number = definedExternally, p: Number = definedExternally, length: Number = definedExternally): BitArray
    fun scrypt(password: String, salt: String, N: Number = definedExternally, r: Number = definedExternally, p: Number = definedExternally, length: Number = definedExternally, Prff: SjclPRFFamilyStatic = definedExternally): BitArray
    fun scrypt(password: String, salt: String): BitArray
    fun scrypt(password: String, salt: String, N: Number = definedExternally): BitArray
    fun scrypt(password: String, salt: String, N: Number = definedExternally, r: Number = definedExternally): BitArray
    fun scrypt(password: String, salt: String, N: Number = definedExternally, r: Number = definedExternally, p: Number = definedExternally): BitArray
    fun scrypt(password: String, salt: String, N: Number = definedExternally, r: Number = definedExternally, p: Number = definedExternally, length: Number = definedExternally): BitArray
}

external open class SjclPRFFamily {
    open fun encrypt(data: BitArray): BitArray
    open fun encrypt(data: String): BitArray
}

external interface SjclHMAC : SjclPRFFamily {
    fun mac(data: BitArray): BitArray
    fun mac(data: String): BitArray
    fun reset()
    fun update(data: BitArray)
    fun update(data: String)
    fun digest(): BitArray
}

external interface SjclPRFFamilyStatic

external interface SjclHMACStatic

external interface `T$2` {
    var c192: SjclEllipticalCurve
    var c224: SjclEllipticalCurve
    var c256: SjclEllipticalCurve
    var c384: SjclEllipticalCurve
    var c521: SjclEllipticalCurve
    var k192: SjclEllipticalCurve
    var k224: SjclEllipticalCurve
    var k256: SjclEllipticalCurve
}

external interface SjclEllipticCurveCryptography {
    var point: SjclEllipticalPointStatic
    var pointJac: SjclPointJacobianStatic
    var curve: SjclEllipticalCurveStatic
    var curves: `T$2`
    fun curveName(curve: SjclEllipticalCurve): String
    fun deserialize(key: SjclECCKeyPairData): dynamic /* SjclECCPublicKey | SjclECCSecretKey */
    var basicKey: SjclECCBasic
    var elGamal: SjclElGamal
    var ecdsa: SjclECDSA
}

external interface SjclEllipticalPoint {
    fun toJac(): SjclPointJacobian
    fun mult(k: BigNumber): SjclEllipticalPoint
    fun mult2(k: BigNumber, k2: BigNumber, affine2: SjclEllipticalPoint): SjclEllipticalPoint
    fun multiples(): Array<SjclEllipticalPoint>
    fun negate(): SjclEllipticalPoint
    fun isValid(): Boolean
    fun toBits(): BitArray
}

external interface SjclEllipticalPointStatic

external interface SjclPointJacobian {
    fun add(T: SjclEllipticalPoint): SjclPointJacobian
    fun doubl(): SjclPointJacobian
    fun toAffine(): SjclEllipticalPoint
    fun mult(k: BigNumber, affine: SjclEllipticalPoint): SjclPointJacobian
    fun mult2(k1: BigNumber, affine: SjclEllipticalPoint, k2: BigNumber, affine2: SjclEllipticalPoint): SjclPointJacobian
    fun negate(): SjclPointJacobian
    fun isValid(): Boolean
}

external interface SjclPointJacobianStatic {
    fun toAffineMultiple(points: Array<SjclPointJacobian>): Array<SjclEllipticalPoint>
}

external interface SjclEllipticalCurve {
    fun fromBits(bits: BitArray): SjclEllipticalPoint
}

external interface SjclEllipticalCurveStatic

external interface SjclKeyPair<P : SjclECCPublicKey, S : SjclECCSecretKey> {
    var pub: P
    var sec: S
}

external interface SjclKeysGenerator<P : SjclECCPublicKey, S : SjclECCSecretKey> { }



external interface SjclECCKeyPairData {
    var type: String
    var secretKey: Boolean
    var point: String
    var curve: String
}

external interface SjclECCPublicKeyData {
    var x: BitArray
    var y: BitArray
}

external open class SjclECCPublicKey {
    open fun serialize(): SjclECCKeyPairData
    open fun get(): SjclECCPublicKeyData
    open fun getType(): String
}

external open class SjclECCSecretKey {
    open fun serialize(): SjclECCKeyPairData
    open fun get(): BitArray
    open fun getType(): String
}

external interface SjclECCPublicKeyFactory<T : SjclECCPublicKey>

external interface SjclECCSecretKeyFactory<T : SjclECCSecretKey>

external interface SjclECCBasic {
    var publicKey: SjclECCPublicKeyFactory<SjclECCPublicKey>
    var secretKey: SjclECCSecretKeyFactory<SjclECCSecretKey>
    fun generateKeys(cn: String): SjclKeysGenerator<SjclECCPublicKey, SjclECCSecretKey>
}

external interface `T$3` {
    var key: BitArray
    var tag: BitArray
}

external open class SjclElGamalPublicKey : SjclECCPublicKey {
    open fun kem(paranoia: Number): `T$3`
}

external open class SjclElGamalSecretKey : SjclECCSecretKey {
    open fun unkem(tag: BitArray): BitArray
    open fun dh(pk: SjclECCPublicKey): BitArray
    open fun dhJavaEc(pk: SjclECCPublicKey): BitArray
}

external interface SjclElGamal {
    var publicKey: SjclECCPublicKeyFactory<SjclElGamalPublicKey>
    var secretKey: SjclECCSecretKeyFactory<SjclElGamalSecretKey>
    var generateKeys: SjclKeysGenerator<SjclElGamalPublicKey, SjclElGamalSecretKey>
}

external open class SjclECDSAPublicKey : SjclECCPublicKey {
    open fun verify(hash: BitArray, rs: BitArray, fakeLegacyVersion: Boolean): Boolean
}

external open class SjclECDSASecretKey : SjclECCSecretKey {
    open fun sign(hash: BitArray, paranoia: Number, fakeLegacyVersion: Boolean, fixedKForTesting: BigNumber = definedExternally): BitArray
}

external interface SjclECDSA {
    var publicKey: SjclECCPublicKeyFactory<SjclECDSAPublicKey>
    var secretKey: SjclECCSecretKeyFactory<SjclECDSASecretKey>
    var generateKeys: SjclKeysGenerator<SjclECDSAPublicKey, SjclECDSASecretKey>
}

external interface SjclRandom {
    fun randomWords(nwords: Number, paranoia: Number = definedExternally): BitArray
    fun setDefaultParanoia(paranoia: Number, allowZeroParanoia: String)
    fun addEntropy(data: Number, estimatedEntropy: Number, source: String)
    fun addEntropy(data: Array<Number>, estimatedEntropy: Number, source: String)
    fun addEntropy(data: String, estimatedEntropy: Number, source: String)
    fun isReady(paranoia: Number = definedExternally): Boolean
    fun getProgress(paranoia: Number = definedExternally): Number
    fun startCollectors()
    fun stopCollectors()
    fun addEventListener(name: String, cb: Function<*>)
    fun removeEventListener(name: String, cb: Function<*>)
}

external interface SjclRandomStatic

external interface SjclKeyExchange {
    var srp: SjclSecureRemotePassword
}

external interface SjclSRPGroup {
    var N: BigNumber
    var g: BigNumber
}

external interface SjclSecureRemotePassword {
    fun makeVerifier(username: String, password: String, salt: BitArray, group: SjclSRPGroup): BitArray
    fun makeX(username: String, password: String, salt: BitArray): BitArray
    fun knownGroup(i: Number): SjclSRPGroup
    fun knownGroup(i: String): SjclSRPGroup
}

external interface SjclCipherParams {
    var v: Number?
        get() = definedExternally
        set(value) = definedExternally
    var iter: Number?
        get() = definedExternally
        set(value) = definedExternally
    var ks: Number?
        get() = definedExternally
        set(value) = definedExternally
    var ts: Number?
        get() = definedExternally
        set(value) = definedExternally
    var mode: String?
        get() = definedExternally
        set(value) = definedExternally
    var adata: String?
        get() = definedExternally
        set(value) = definedExternally
    var cipher: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface SjclCipherEncryptParams : SjclCipherParams {
    var salt: BitArray
    var iv: BitArray
}

external interface SjclCipherDecryptParams : SjclCipherParams {
    var salt: BitArray?
        get() = definedExternally
        set(value) = definedExternally
    var iv: BitArray?
        get() = definedExternally
        set(value) = definedExternally
}

external interface SjclCipherEncrypted : SjclCipherEncryptParams {
    var kemtag: BitArray?
        get() = definedExternally
        set(value) = definedExternally
    var ct: BitArray
}

external interface SjclCipherDecrypted : SjclCipherEncrypted {
    var key: BitArray
}

external interface SjclConvenienceEncryptor { }


external interface SjclConvenienceDecryptor { }

external interface SjclJson {
    var encrypt: SjclConvenienceEncryptor
    var decrypt: SjclConvenienceDecryptor
    fun encode(obj: Any): String
    fun decode(obj: String): Any
}