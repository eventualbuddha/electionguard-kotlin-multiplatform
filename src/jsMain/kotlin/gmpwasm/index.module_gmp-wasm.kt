@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")

package gmpwasm

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

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