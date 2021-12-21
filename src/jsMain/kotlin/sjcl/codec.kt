@file:JsModule("sjcl")
@file:JsQualifier("sjcl.codec.bytes")
package sjcl.codec.bytes

import sjcl.bitArray

external fun fromBits(bitArray: bitArray): ByteArray
external fun toBits(input: ByteArray): bitArray