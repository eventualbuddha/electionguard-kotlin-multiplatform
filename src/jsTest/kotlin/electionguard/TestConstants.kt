package electionguard

import electionguard.Base64.fromSafeBase64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestConstants {
    @Test
    fun saneConstantsBig() {
        val p = b64ProductionP.fromSafeBase64().toBigInteger()
        val q = b64ProductionQ.fromSafeBase64().toBigInteger()
        val qInv = b64ProductionP256MinusQ.fromSafeBase64().toBigInteger()
        val g = b64ProductionG.fromSafeBase64().toBigInteger()
        val r = b64ProductionR.fromSafeBase64().toBigInteger()

        val big1 = 1U.toBigInteger()

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
        val p = b64TestP.fromSafeBase64().toBigInteger()
        val q = b64TestQ.fromSafeBase64().toBigInteger()
        val g = b64TestG.fromSafeBase64().toBigInteger()
        val r = b64TestR.fromSafeBase64().toBigInteger()

        assertEquals(intTestP.toUInt().toBigInteger(), p)
        assertEquals(intTestQ.toUInt().toBigInteger(), q)
        assertEquals(intTestG.toUInt().toBigInteger(), g)
        assertEquals(intTestR.toUInt().toBigInteger(), r)
    }
}