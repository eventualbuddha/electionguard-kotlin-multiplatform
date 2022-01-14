package electionguard.core

import electionguard.core.Base64.fromSafeBase64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestConstants {
    @Test
    fun saneConstantsBig() {
        runTest {
            val gmp = getGmpContext()
            console.log("saneConstants: successfully got context")
            val p = gmp.byteArrayToBigInteger(b64ProductionP.fromSafeBase64())
            console.log("saneConstants: successfully used context")
            val q = gmp.byteArrayToBigInteger(b64ProductionQ.fromSafeBase64())
            val qInv = gmp.byteArrayToBigInteger(b64ProductionP256MinusQ.fromSafeBase64())
            val g = gmp.byteArrayToBigInteger(b64ProductionG.fromSafeBase64())
            val r = gmp.byteArrayToBigInteger(b64ProductionR.fromSafeBase64())

            val big1 = gmp.numberToBigInteger(1)

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
            val gmp = getGmpContext()
            console.log("saneConstantsSmall: successfully got context")
            val p = gmp.byteArrayToBigInteger(b64TestP.fromSafeBase64())
            console.log("saneConstantsSmall: successfully used context")
            val q = gmp.byteArrayToBigInteger(b64TestQ.fromSafeBase64())
            val g = gmp.byteArrayToBigInteger(b64TestG.fromSafeBase64())
            val r = gmp.byteArrayToBigInteger(b64TestR.fromSafeBase64())

            assertEquals(gmp.numberToBigInteger(intTestP), p)
            assertEquals(gmp.numberToBigInteger(intTestQ), q)
            assertEquals(gmp.numberToBigInteger(intTestG), g)
            assertEquals(gmp.numberToBigInteger(intTestR), r)
        }
    }
}