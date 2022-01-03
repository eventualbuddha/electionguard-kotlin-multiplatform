package electionguard

import electionguard.Base64.fromSafeBase64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestConstants {
    @Test
    fun saneConstantsBig() {
        runTest {
            val context = productionGroup()
            val p = context.gmpContext.byteArrayToBigInteger(b64ProductionP.fromSafeBase64())
            val q = context.gmpContext.byteArrayToBigInteger(b64ProductionQ.fromSafeBase64())
            val qInv = context.gmpContext.byteArrayToBigInteger(b64ProductionP256MinusQ.fromSafeBase64())
            val g = context.gmpContext.byteArrayToBigInteger(b64ProductionG.fromSafeBase64())
            val r = context.gmpContext.byteArrayToBigInteger(b64ProductionR.fromSafeBase64())

            val big1 = context.gmpContext.one

            assertTrue(p > big1)
            assertTrue(q > big1)
            assertTrue(g > big1)
            assertTrue(r > big1)
            assertTrue(qInv > big1)
            assertTrue(q < p)
            assertTrue(g < p)
        }
    }

    @Test
    fun saneConstantsSmall() {
        runTest {
            val context = productionGroup()
            val p = context.gmpContext.byteArrayToBigInteger(b64TestP.fromSafeBase64())
            val q = context.gmpContext.byteArrayToBigInteger(b64TestQ.fromSafeBase64())
            val g = context.gmpContext.byteArrayToBigInteger(b64TestG.fromSafeBase64())
            val r = context.gmpContext.byteArrayToBigInteger(b64TestR.fromSafeBase64())

            assertEquals(context.gmpContext.numberToBigInteger(intTestP), p)
            assertEquals(context.gmpContext.numberToBigInteger(intTestQ), q)
            assertEquals(context.gmpContext.numberToBigInteger(intTestG), g)
            assertEquals(context.gmpContext.numberToBigInteger(intTestR), r)
        }
    }
}