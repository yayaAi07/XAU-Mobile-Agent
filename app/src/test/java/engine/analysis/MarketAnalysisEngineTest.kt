package engine.analysis

import engine.data.Candle
import engine.data.MarketSnapshot
import engine.data.Timeframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketAnalysisEngineTest {

    @Test
    fun analyze_returns_valid_result() {
        val snapshot = createSnapshot()

        val result = MarketAnalysisEngine.analyze(snapshot)

        assertTrue(result.overallStrength in 0..100)
        assertTrue(result.trend.strength in 0..100)
        assertTrue(result.structure.strength in 0..100)
        assertTrue(result.location.strength in 0..100)
        assertTrue(result.ad.strength in 0..100)
    }

    @Test
    fun analyze_resolves_market_state_from_trend_and_structure() {
        val snapshot = createSnapshot()

        val result = MarketAnalysisEngine.analyze(snapshot)

        assertEquals(
            resolveExpectedMarketState(
                result.trend.direction,
                result.structure.direction
            ),
            result.marketState
        )
    }

    @Test
    fun analyze_overall_strength_uses_all_components() {
        val snapshot = createSnapshot()

        val result = MarketAnalysisEngine.analyze(snapshot)

        val expected =
            (
                result.trend.strength * 0.30 +
                result.structure.strength * 0.30 +
                result.location.strength * 0.20 +
                result.ad.strength * 0.20
            ).toInt().coerceIn(0, 100)

        assertEquals(expected, result.overallStrength)
    }

    private fun resolveExpectedMarketState(
        trend: TrendDirection,
        structure: StructureDirection
    ): MarketState {
        return when {
            trend == TrendDirection.UP &&
                structure == StructureDirection.BULLISH ->
                MarketState.TREND_UP

            trend == TrendDirection.DOWN &&
                structure == StructureDirection.BEARISH ->
                MarketState.TREND_DOWN

            trend == TrendDirection.SIDEWAYS ->
                MarketState.RANGE

            trend == TrendDirection.UNCERTAIN ||
                structure == StructureDirection.UNCERTAIN ->
                MarketState.UNCERTAIN

            else ->
                MarketState.RANGE
        }
    }

    private fun createSnapshot(): MarketSnapshot {

        return MarketSnapshot(
            symbol = "XAUUSD",
            capturedAt = System.currentTimeMillis(),

            h4 = createCandles(
                timeframe = Timeframe.H4,
                startPrice = 2000.0,
                count = 10
            ),

            h1 = createCandles(
                timeframe = Timeframe.H1,
                startPrice = 2000.0,
                count = 10
            ),

            m15 = createCandles(
                timeframe = Timeframe.M15,
                startPrice = 2000.0,
                count = 10
            ),

            m5 = createCandles(
                timeframe = Timeframe.M5,
                startPrice = 2000.0,
                count = 10
            ),

            m1 = createCandles(
                timeframe = Timeframe.M1,
                startPrice = 2000.0,
                count = 10
            )
        )
    }

    private fun createCandles(
        timeframe: Timeframe,
        startPrice: Double,
        count: Int
    ): List<Candle> {

        return (0 until count).map { index ->

            val price = startPrice + index * 2.0

            Candle(
                timestamp = index + 1L,
                open = price,
                high = price + 1.0,
                low = price - 0.5,
                close = price + 0.5,
                volume = 100.0,
                timeframe = timeframe
            )
        }
    }
}
