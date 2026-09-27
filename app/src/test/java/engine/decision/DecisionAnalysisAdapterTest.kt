package engine.decision

import engine.data.MarketDataRepository
import engine.data.MockMarketDataSource
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionAnalysisAdapterTest {

    @Test
    fun adapter_can_analyze_market_snapshot() {

        val repository =
            MarketDataRepository(
                MockMarketDataSource()
            )

        val adapter =
            DecisionAnalysisAdapter(
                repository = repository,
                symbol = "XAUUSD"
            )

        val decision =
            adapter.analyze(
                now = System.currentTimeMillis()
            )

        if (decision != null) {
            assertTrue(decision.symbol == "XAUUSD")
            assertTrue(decision.analysisConfidence in 0..100)
            assertTrue(decision.expiresAt > decision.createdAt)
            assertTrue(decision.reasonCodes.isNotEmpty())
        }
    }
}
