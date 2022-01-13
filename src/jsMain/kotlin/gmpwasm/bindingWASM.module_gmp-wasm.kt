@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
@file:JsModule("gmp-wasm")
@file:JsNonModule

// async function returning a JS object with a heap and a list of "exports" generated from the WASM guts
external var getBinding: suspend (reset: Boolean) -> dynamic