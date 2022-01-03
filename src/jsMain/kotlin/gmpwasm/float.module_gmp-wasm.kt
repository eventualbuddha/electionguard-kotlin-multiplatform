@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
@file:JsModule("gmp-wasm")

package gmpwasm

external interface FloatFactory : `T$1`

external interface Float : FloatReturn

external enum class FloatRoundingMode {
    ROUND_NEAREST /* = 0 */,
    ROUND_TO_ZERO /* = 1 */,
    ROUND_UP /* = 2 */,
    ROUND_DOWN /* = 3 */,
    ROUND_FROM_ZERO /* = 4 */
}

external interface FloatOptions {
    var precisionBits: Number?
        get() = definedExternally
        set(value) = definedExternally
    var roundingMode: FloatRoundingMode?
        get() = definedExternally
        set(value) = definedExternally
    var radix: Number?
        get() = definedExternally
        set(value) = definedExternally
}

external interface `T$0` {
    var mpfr_t: Number
    var precisionBits: Number
    var rndMode: Number
    var radix: Number
    var type: String
    var options: FloatOptions
    var setOptions: FloatOptions
    fun add(param_val: Integer): Float
    fun add(param_val: Rational): Float
    fun add(param_val: Float): Float
    fun add(param_val: String): Float
    fun add(param_val: Number): Float
    fun sub(param_val: Integer): Float
    fun sub(param_val: Rational): Float
    fun sub(param_val: Float): Float
    fun sub(param_val: String): Float
    fun sub(param_val: Number): Float
    fun mul(param_val: Integer): Float
    fun mul(param_val: Rational): Float
    fun mul(param_val: Float): Float
    fun mul(param_val: String): Float
    fun mul(param_val: Number): Float
    fun div(param_val: Integer): Float
    fun div(param_val: Rational): Float
    fun div(param_val: Float): Float
    fun div(param_val: String): Float
    fun div(param_val: Number): Float
    fun sqrt(): Float
    fun invSqrt(): Float
    fun cbrt(): Float
    fun nthRoot(nth: Number): Float
    fun neg(): Float
    fun abs(): Float
    fun factorial(): Any
    fun isInteger(): Boolean
    fun isZero(): Boolean
    fun isRegular(): Boolean
    fun isNumber(): Boolean
    fun isInfinite(): Boolean
    fun isNaN(): Boolean
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
    fun ln(): Float
    fun log2(): Float
    fun log10(): Float
    fun exp(): Float
    fun exp2(): Float
    fun exp10(): Float
    fun pow(param_val: Float): Float
    fun pow(param_val: Number): Float
    fun sin(): Float
    fun cos(): Float
    fun tan(): Float
    fun sec(): Float
    fun csc(): Float
    fun cot(): Float
    fun acos(): Float
    fun asin(): Float
    fun atan(): Float
    fun sinh(): Float
    fun cosh(): Float
    fun tanh(): Float
    fun sech(): Float
    fun csch(): Float
    fun coth(): Float
    fun acosh(): Float
    fun asinh(): Float
    fun atanh(): Float
    fun eint(): Float
    fun li2(): Float
    fun gamma(): Float
    fun lngamma(): Float
    fun digamma(): Float
    fun beta(op2: Float): Float
    fun zeta(): Float
    fun erf(): Float
    fun erfc(): Float
    fun j0(): Float
    fun j1(): Float
    fun jn(n: Number): Float
    fun y0(): Float
    fun y1(): Float
    fun yn(n: Number): Float
    fun agm(op2: Float): Float
    fun ai(): Float
    fun sign(): Number
    fun toNumber(): Number
    fun ceil(): Float
    fun floor(): Float
    fun round(): Float
    fun roundEven(): Float
    fun trunc(): Float
    fun roundTo(prec: Number): Float
    fun frac(): Float
    fun fmod(y: Float): Float
    fun remainder(y: Float): Float
    fun nextAbove(): Float
    fun nextBelow(): Float
    fun exponent2(): Number
    fun toString(radix: Number = definedExternally): String
    fun toFixed(digits: Number = definedExternally, radix: Number = definedExternally): Any
    fun toInteger(): Integer
    fun toRational(): Rational
}

external interface `T$1` {
    var Float: (param_val: dynamic /* String? | Number? | Float? | Rational? | Integer? */, options: FloatOptions) -> `T$0`
    var isFloat: (param_val: Any) -> Boolean
    var Pi: (options: FloatOptions) -> `T$0`
    var EulerConstant: (options: FloatOptions) -> `T$0`
    var EulerNumber: (options: FloatOptions) -> Float
    var Log2: (options: FloatOptions) -> `T$0`
    var Catalan: (options: FloatOptions) -> `T$0`
    var destroy: () -> Unit
}

external fun getFloatContext(gmp: GMPFunctions, ctx: Any, ctxOptions: FloatOptions = definedExternally): `T$1`