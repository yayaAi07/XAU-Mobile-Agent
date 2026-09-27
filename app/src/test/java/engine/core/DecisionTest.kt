
package engine.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionTest {

    @Test
    fun testDecisionExpiration() {
        val decision = Decision(
            decisionId = "test-1",
            symbol = "XAUUSD",
            direction = Direction.BUY,
            createdAt = 1000L,
            expiresAt = 2000L,
            sourceTimeframe = SourceTimeframe.M5,
            analysisConfidence = 80,
            marketState = "NORMAL",
            reasonCodes = emptyList()
        )

        assertFalse(decision.isExpiredAt(1500L))
        assertTrue(decision.isExpiredAt(2000L))
        assertEquals(
            500L,
            decision.remainingLifetimeMillis(1500L)
        )
    }

    @Test
    fun testConfidenceValidation() {
        try {
            Decision(
                decisionId = "test-2",
                symbol = "XAUUSD",
                direction = Direction.SELL,
                createdAt = 1000L,
                expiresAt = 2000L,
                sourceTimeframe = SourceTimeframe.M15,
                analysisConfidence = 150,
                marketState = "NORMAL",
                reasonCodes = emptyList()
            )

            throw AssertionError(
                "Should have thrown IllegalArgumentException"
            )

        } catch (e: IllegalArgumentException) {

            assertTrue(
                e.message!!.contains("analysisConfidence")
            )
        }
    }
}
