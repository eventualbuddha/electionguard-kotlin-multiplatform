package electionguard

import com.soywiz.krypto.encoding.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestConstants {
    @Test
    fun saneConstantsBig() {
        val context = productionGroup(PowRadixOption.NO_ACCELERATION)
        val p = b64ProductionP.fromBase64().toBigInteger(context.gmpLib)
        val q = b64ProductionQ.fromBase64().toBigInteger(context.gmpLib)
        val qInv = b64ProductionP256MinusQ.fromBase64().toBigInteger(context.gmpLib)
        val g = b64ProductionG.fromBase64().toBigInteger(context.gmpLib)
        val r = b64ProductionR.fromBase64().toBigInteger(context.gmpLib)

        val big1 = context.gmpLib.ctx.Integer(1)

        assertTrue(p.greaterThan(big1))
        assertTrue(q.greaterThan(big1))
        assertTrue(g.greaterThan(big1))
        assertTrue(r.greaterThan(big1))
        assertTrue(qInv.greaterThan(big1))
        assertTrue(q.lessThan(p))
        assertTrue(g.lessThan(p))
    }

    @Test
    fun saneConstantsSmall() {
        val context = testGroup()

        val p = b64TestP.fromBase64().toBigInteger(context.gmpLib)
        val q = b64TestQ.fromBase64().toBigInteger(context.gmpLib)
        val g = b64TestG.fromBase64().toBigInteger(context.gmpLib)
        val r = b64TestR.fromBase64().toBigInteger(context.gmpLib)

        assertEquals(context.gmpLib.ctx.Integer(intTestP), p)
        assertEquals(context.gmpLib.ctx.Integer(intTestQ), q)
        assertEquals(context.gmpLib.ctx.Integer(intTestG), g)
        assertEquals(context.gmpLib.ctx.Integer(intTestR), r)
    }
}