@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")

package gmpwasm

import org.khronos.webgl.Uint8Array

external fun decodeBase64(data: String): Uint8Array