package electionguard.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals

class DLogTest {
    private val small = 2_000

    @Test
    fun basics() {
        runTest {
            checkAll(propTestFastConfig, Arb.int(min=0, max=small)) {
                val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)
                val result = context.dLog(context.gPowP(it.toElementModQ(context)))

                assertEquals(it, result, "dLog failure: $it != $result")
            }
        }
    }
}
