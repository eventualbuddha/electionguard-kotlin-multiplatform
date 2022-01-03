@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
@file:JsModule("gmp-wasm")

package gmpwasm

import org.khronos.webgl.Uint8Array

external interface IntegerFactory : IntegerInterface

external interface Integer : IntegerReturn

external enum class DivMode {
    CEIL /* = 0 */,
    FLOOR /* = 1 */,
    TRUNCATE /* = 2 */
}

external interface `T$3`<T, T_1, T_2, T_3> {
    var mpz_t: Number
    var type: String
    fun <T> add(param_val: T): IntegerOutputType<T>
    fun <T_1> sub(param_val: T_1): IntegerOutputType<T_1>
    fun <T_2> mul(param_val: T_2): IntegerOutputType<T_2>
    fun neg(): Integer
    fun abs(): Integer
    fun <T_3> div(param_val: T_3, mode: DivMode = definedExternally): IntegerOutputType<T_3>
    fun pow(exp: Rational, mod: Integer = definedExternally): Integer
    fun pow(exp: Rational): Integer
    fun pow(exp: Rational, mod: Number = definedExternally): Integer
    fun pow(exp: Integer, mod: Integer = definedExternally): Integer
    fun pow(exp: Integer): Integer
    fun pow(exp: Integer, mod: Number = definedExternally): Integer
    fun pow(exp: Number, mod: Integer = definedExternally): Integer
    fun pow(exp: Number): Integer
    fun pow(exp: Number, mod: Number = definedExternally): Integer
    fun sqrt(): Integer
    fun nthRoot(nth: Number): Integer
    fun factorial(): Integer
    fun doubleFactorial(): Integer
    fun isPrime(reps: Number = definedExternally): dynamic /* Boolean | "probably-prime" */
    fun nextPrime(): Integer
    fun gcd(param_val: Integer): Integer
    fun gcd(param_val: Number): Integer
    fun lcm(param_val: Integer): Integer
    fun lcm(param_val: Number): Integer
    fun complement1(): Integer
    fun complement2(): Integer
    fun and(param_val: Integer): Integer
    fun and(param_val: Number): Integer
    fun or(param_val: Integer): Integer
    fun or(param_val: Number): Integer
    fun xor(param_val: Integer): Integer
    fun xor(param_val: Number): Integer
    fun shiftLeft(param_val: Number): Integer
    fun shiftRight(param_val: Number): Integer
    fun setBit(i: Number): Integer
    fun setBits(indices: Array<Number>): Integer
    fun clearBit(index: Number): Integer
    fun clearBits(indices: Array<Number>): Integer
    fun flipBit(index: Number): Integer
    fun flipBits(indices: Array<Number>): Integer
    fun getBit(index: Number): Number
    fun msbPosition(): Number
    fun sliceBits(start: Number = definedExternally, end: Number = definedExternally): Integer
    fun writeTo(num: Integer, offset: Number = definedExternally, bitCount: Number = definedExternally): Any
    fun isEqual(param_val: Integer): Boolean
    fun isEqual(param_val: Rational): Boolean
    fun isEqual(param_val: Float): Boolean
    fun isEqual(param_val: String): Boolean
    fun isEqual(param_val: Number): Boolean
    fun lessThan(param_val: Integer): Boolean
    fun lessThan(param_val: Rational): Boolean
    fun lessThan(param_val: Float): Boolean
    fun lessThan(param_val: String): Boolean
    fun lessThan(param_val: Number): Boolean
    fun lessOrEqual(param_val: Integer): Boolean
    fun lessOrEqual(param_val: Rational): Boolean
    fun lessOrEqual(param_val: Float): Boolean
    fun lessOrEqual(param_val: String): Boolean
    fun lessOrEqual(param_val: Number): Boolean
    fun greaterThan(param_val: Integer): Boolean
    fun greaterThan(param_val: Rational): Boolean
    fun greaterThan(param_val: Float): Boolean
    fun greaterThan(param_val: String): Boolean
    fun greaterThan(param_val: Number): Boolean
    fun greaterOrEqual(param_val: Integer): Boolean
    fun greaterOrEqual(param_val: Rational): Boolean
    fun greaterOrEqual(param_val: Float): Boolean
    fun greaterOrEqual(param_val: String): Boolean
    fun greaterOrEqual(param_val: Number): Boolean
    fun sign(): Number
    fun toNumber(): Number
    fun toBuffer(littleEndian: Boolean = definedExternally): Uint8Array
    fun toString(radix: Number = definedExternally): String
    fun toRational(): Rational
    fun toFloat(): Float
}

external interface IntegerInterface {
    var Integer: (num: dynamic /* String | Number | Integer | Uint8Array | Rational | Float */, radix: Number) -> `T$3`<Any?, Any?, Any?, Any?>
    var isInteger: (param_val: Any) -> Boolean
    var destroy: () -> Unit
}

external fun getIntegerContext(gmp: GMPFunctions, ctx: Any): IntegerInterface