@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
package sjcl.TypeHelpers

import sjcl.BigNumber

// the original output from Dukat was trying to deal with the union types that sjcl accepts
// (String, Number, or BigNumber), which didn't compile. The solution, here, is to break
// this down to just BigNumber, which is all that we need.

external interface One<T>

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> One<T>.invoke(value: T): BigNumber {
    return asDynamic()(value)
}

external interface BigNumberBinaryOperator : One<BigNumber>

external interface Two<T1, T2>

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T1, T2> Two<T1, T2>.invoke(x: T1, N: T2): BigNumber {
    return asDynamic()(x, N)
}

external interface Bind1<T> : Two<BigNumber, T>

external interface BigNumberTrinaryOperator : Bind1<BigNumber>