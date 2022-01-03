@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")

package gmpwasm

import kotlin.js.Promise

external interface CalculateType {
    var Integer: IntegerFactory
    var Rational: RationalFactory
    var Float: FloatFactory
    var Pi: (options: FloatOptions) -> Float
    var EulerConstant: (options: FloatOptions) -> Float
    var EulerNumber: (options: FloatOptions) -> Float
    var Log2: (options: FloatOptions) -> Float
    var Catalan: (options: FloatOptions) -> Float
}

external interface CalculateTypeWithDestroy : CalculateType {
    var destroy: () -> Unit
}

external interface GMPLib {
    var binding: GMPFunctions
    var calculate: (fn: (gmp: CalculateType) -> dynamic, options: CalculateOptions) -> Unit
    var getContext: (options: CalculateOptions) -> CalculateTypeWithDestroy
    var reset: () -> Promise<Unit>
}

external interface CalculateOptions : FloatOptions

external fun init(): Promise<GMPLib>

external var precisionToBits: (digits: Number) -> Number