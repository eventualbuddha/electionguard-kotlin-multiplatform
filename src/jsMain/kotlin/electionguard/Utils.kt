package electionguard

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

// complicated browser-vs-node detection logic from here:
// https://github.com/flexdinesh/browser-or-node/blob/master/src/index.js

// const isBrowser =
//  typeof window !== "undefined" && typeof window.document !== "undefined";
//
//const isNode =
//  typeof process !== "undefined" &&
//  process.versions != null &&
//  process.versions.node != null;

/** Distinguish if we're in Node.js (true) or maybe in a browser (false). */
fun isNodeJs(): Boolean =
    jsTypeOf(js("process")) != "undefined" &&
            js("process").versions != null &&
            js("process").versions.node != null

/** Distinguish if we're in a browser with crypto built-in (true) or maybe somewhere else (false). */
fun isBrowser(): Boolean =
    jsTypeOf(js("window")) != "undefined" &&
            jsTypeOf(js("window.crypto")) != "undefined"

/** Copy from a Kotlin ByteArray to JavaScript's intrinsic Uint8Array. */
fun ByteArray.toUint8Array(): Uint8Array {
    val result = Uint8Array(this.size)
    for (i in 0..(this.size - 1)) {
        result[i] = this[i]
    }
    return result
}

/** Copy from a JavaScript intrinsic Uint8Array to a Kotlin ByteArray. */
fun Uint8Array.toByteArray() = ByteArray(this.length) { this[it] }