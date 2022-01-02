@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")

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
import tsstdlib.ReturnType

external interface RationalFactory : RationalFactoryReturn

typealias RationalReturn = ReturnType<Any>

external interface Rational : RationalReturn

typealias OutputType<T> = Any

external interface `T$5`<T, T_1, T_2, T_3> {
    var mpq_t: Number
    var type: String
    fun <T> add(param_val: T): OutputType<T>
    fun <T_1> sub(param_val: T_1): OutputType<T_1>
    fun <T_2> mul(param_val: T_2): OutputType<T_2>
    fun neg(): Rational
    fun invert(): Rational
    fun abs(): Rational
    fun <T_3> div(param_val: T_3): OutputType<T_3>
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
    fun numerator(): Integer
    fun denominator(): Integer
    fun sign(): Number
    fun toNumber(): Number
    override fun toString(): String
    fun toInteger(): Integer
    fun toFloat(): Float
}

external interface `T$6` {
    var Rational: (p1: dynamic /* String | Number | Rational | Integer */, p2: dynamic /* String | Number | Integer */) -> `T$5`<Any?, Any?, Any?, Any?>
    var isRational: (param_val: Any) -> Boolean
    var destroy: () -> Unit
}

external fun getRationalContext(gmp: GMPFunctions, ctx: Any): `T$6`