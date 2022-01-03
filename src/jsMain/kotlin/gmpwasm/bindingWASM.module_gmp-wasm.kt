@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
@file:JsModule("gmp-wasm")

import kotlin.js.Promise

external var getBinding: (reset: Boolean) -> Promise<Any>