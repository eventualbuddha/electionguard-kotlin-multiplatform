@file:OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)

package electionguard

import kotlinx.coroutines.*
import kotlin.js.Promise

/** Hack to take any code block that returns a [Promise] and instead get the value inside. */
fun <T> getPromise(f: suspend () -> Promise<T>): T =
    GlobalScope.async {
        f().await()
    }.getCompleted()