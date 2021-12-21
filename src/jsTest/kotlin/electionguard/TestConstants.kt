package electionguard

import electionguard.Base64.fromSafeBase64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestConstants {
    @Test
    fun saneConstantsBig() {
        val p = b64ProductionP.fromSafeBase64().toBigNumber()
        val q = b64ProductionQ.fromSafeBase64().toBigNumber()
        val qInv = b64ProductionP256MinusQ.fromSafeBase64().toBigNumber()
        val g = b64ProductionG.fromSafeBase64().toBigNumber()
        val r = b64ProductionR.fromSafeBase64().toBigNumber()

        val big1 = 1U.toBigNumber()

        assertTrue(p > big1)
        assertTrue(q > big1)
        assertTrue(g > big1)
        assertTrue(r > big1)
        assertTrue(qInv > big1)
        assertTrue(q < p)
        assertTrue(g < p)
    }

    @Test
    fun saneConstantsSmall() {
        val p = b64TestP.fromSafeBase64().toBigNumber()
        val q = b64TestQ.fromSafeBase64().toBigNumber()
        val g = b64TestG.fromSafeBase64().toBigNumber()
        val r = b64TestR.fromSafeBase64().toBigNumber()

        assertEquals(intTestP.toUInt().toBigNumber(), p)
        assertEquals(intTestQ.toUInt().toBigNumber(), q)
        assertEquals(intTestG.toUInt().toBigNumber(), g)
        assertEquals(intTestR.toUInt().toBigNumber(), r)
    }
}