@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
package sjcl.TypeHelpers

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
import sjcl.BigNumber

// The Dukat origin had significant problems compiling with Kotlin/JS, so we had
// to make some changes here to get rid of the union types.

external interface One<T> { }

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> One<T>.invoke(value: T): BigNumber =
    asDynamic()(value).unsafeCast<BigNumber>()

//external interface BigNumberBinaryOperator : One<Number>, One<String>, One<BigNumber>
external interface BigNumberBinaryOperator : One<BigNumber>

external interface Two<T1, T2> { }

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T1, T2> Two<T1, T2>.invoke(x: T1, N: T2): BigNumber =
    asDynamic()(x, N).unsafeCast<BigNumber>()

//external interface Bind1<T> : Two<Number, T>, Two<String, T>, Two<BigNumber, T>
external interface Bind1<T> : Two<BigNumber, T>

//external interface BigNumberTrinaryOperator : Bind1<Number>, Bind1<String>, Bind1<BigNumber>
external interface BigNumberTrinaryOperator : Bind1<BigNumber>
