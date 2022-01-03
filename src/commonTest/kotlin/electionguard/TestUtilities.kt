@file:OptIn(ExperimentalCoroutinesApi::class)

package electionguard

import io.kotest.assertions.fail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

/**
 * Kotest requires its properties to be executed as a suspending function, so we're using a feature
 * from `kotlinx-coroutines-test` to make it happen. Note that the internal `runTest` call requires
 * that it be called *at most once per test method*. It's fine to put multiple asserts or `forAll`
 * calls or whatever else inside the lambda body.
 */
fun runTest(f: suspend () -> Unit) {
    // another benefit of having this wrapper code: we don't have to have the OptIn thing
    // at the top of every unit test file
    runTest { f() }
}

/** Verifies that the lambda body throws the specified exception or error. */
inline fun <reified T : Throwable> assertThrows(message: String = "", f: () -> Unit) {
    try {
        f()
    } catch (ex: Throwable) {
        if (ex is T) {
            return
        }
    }
    fail(message)
}

/** Verifies that the lambda body throws no exceptions. */
inline fun assertDoesNotThrow(message: String = "", f: () -> Unit) {
    try {
        f()
    } catch (ex: Throwable) {
        fail(message)
    }
}
