package electionguard

import kotlin.test.Test
import kotlin.test.assertEquals

class NoncesTest {
    @Test
    fun sequencesAreLazy() {
        // TODO: add a timeout on this test; if it takes more than a few seconds,
        //   then it's failed. We could use @Timeout with JUnit5 Jupipter, but
        //   that's not available in Kotlin Test.

        // Possibly relevant:
        //   https://medium.com/@ralf.stuckert/testing-coroutines-timeout-36c22db1b06a

        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            val nonces = Nonces(context.ONE_MOD_Q, "sample_text")
            val expected2 = nonces.asPair().toList()
            val expected3 = nonces.asTriple().toList()

            // If there was eager rather than lazy behavior, this would take forever
            // and the test timeout would activate.
            val actual2 = nonces.asSequence().take(2).toList()
            val actual3 = nonces.asSequence().take(3).toList()

            assertEquals(expected2, actual2)
            assertEquals(expected3, actual3)
        }
    }

    @Test
    fun noncesSupportDestructuring() {
        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            val nonces = Nonces(context.ONE_MOD_Q, "sample_text")
            val expected0 = nonces[0]
            val expected1 = nonces[1]
            val (actual0, actual1) = nonces.asPair()

            assertEquals(expected0, actual0)
            assertEquals(expected1, actual1)

            val (also0, also1) = nonces
            assertEquals(expected0, also0)
            assertEquals(expected1, also1)
        }
    }
}
