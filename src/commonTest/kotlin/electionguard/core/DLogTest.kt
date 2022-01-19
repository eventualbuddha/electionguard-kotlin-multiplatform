package electionguard.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals

class DLogTest {
    private val small = 2_000

    @Test
    fun evenMoreBasic() {
        runTest {
            val context = productionGroup(PowRadixOption.NO_ACCELERATION)

            val result0 = context.dLog(context.gPowP(0.toElementModQ(context)))
            assertEquals(0, result0)

            val result1 = context.dLog(context.gPowP(1.toElementModQ(context)))
            assertEquals(1, result1)

            val result2 = context.dLog(context.gPowP(2.toElementModQ(context)))
            assertEquals(2, result2)
        }
    }

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
