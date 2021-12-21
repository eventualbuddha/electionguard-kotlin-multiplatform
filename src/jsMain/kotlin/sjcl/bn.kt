@file:JsModule("sjcl")
@file:JsQualifier("sjcl")
package sjcl

external class bn(input: dynamic) {
    fun greaterEquals(other: bn): Boolean
    fun mod(n: bn): bn
    fun inverseMod(n: bn): bn
    fun add(n: bn): bn
    fun sub(n: bn): bn
    fun mul(n: bn): bn
    fun mulmod(b: bn, n: bn): bn
    fun powermod(e: bn, n: bn): bn
    fun toBits(): bitArray
}
