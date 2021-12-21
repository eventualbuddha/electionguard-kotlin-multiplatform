package sjcl

@JsModule("sjcl.codec.bytes")
external fun fromBits(bitArray: bitArray): ByteArray
external fun toBits(input: ByteArray): bitArray