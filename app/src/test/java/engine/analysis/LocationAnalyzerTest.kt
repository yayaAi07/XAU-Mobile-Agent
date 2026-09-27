package engine.analysis

import engine.data.Candle
import engine.data.MarketSnapshot
import engine.data.Timeframe
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationAnalyzerTest {

    @Test
    fun empty_h4_returns_uncertain() {
        val snapshot = createSnapshot(
            h4 = emptyList(),
            h1 = candles(
                low = 100.0,
                high = 110.0,
                close = 105.0,
                timeframe = Timeframe.H1
            )
        )

        val result = LocationAnalyzer.analyze(snapshot)

        assertEquals(PriceLocation.UNCERTAIN, result.location)
        assertEquals(0, result.strength)
    }

    @Test
    fun empty_h1_returns_uncertain() {
        val snapshot = createSnapshot(
            h4 = candles(
                low = 100.0,
                high = 110.0,
                close = 105.0,
                timeframe = Timeframe.H4
            ),
            h1 = emptyList()
        )

        val result = LocationAnalyzer.analyze(snapshot)

        assertEquals(PriceLocation.UNCERTAIN, result.location)
        assertEquals(0, result.strength)
    }

    @Test
    fun zero_h4_range_returns_uncertain() {
        val snapshot = createSnapshot(
            h4 = candles(
                low = 100.0,
                high = 100.0,
                close = 100.0,
                timeframe = Timeframe.H4
            ),
            h1 = candles(
                low = 90.0,
                high = 110.0,
                close = 100.0,
                timeframe = Timeframe.H1
            )
        )

        val result = LocationAnalyzer.analyze(snapshot)

        assertEquals(PriceLocation.UNCERTAIN, result.location)
        assertEquals(0, result.strength)
    }

    @Test
    fun zero_h1_range_returns_uncertain() {
        val snapshot = createSnapshot(
            h4 = candles(
                low = 90.0,
                high = 110.0,
                close = 100.0,
                timeframe = Timeframe.H4
            ),
            h1 = candles(
                low = 100.0,
                high = 100.0,
                close = 100.0,
                timeframe = Timeframe.H1
            )
        )

        val result = LocationAnalyzer.analyze(snapshot)

        assertEquals(PriceLocation.UNCERTAIN, result.location)
        assertEquals(0, result.strength)
    }

    @Test
    fun price_in_lower_quarter_returns_discount() {
        val snapshot = createSnapshot(
            h4 = candles(
                low = 100.0,
                high = 200.0,
                close = 110.0,
                timeframe = Timeframe.H4
            ),
            h1 = candles(
                low = 100.0,
                high = 200.0,
                close = 110.0,
                timeframe = Timeframe.H1
            )
        )

        val result = LocationAnalyzer.analyze(snapshot)

        assertEquals(PriceLocation.DISCOUNT, result.location)
        assertEquals(75, result.strength)
    }

    @Test
    fun price_in_upper_quarter_returns_premium() {
        val snapshot = createSnapshot(
            h4 = candles(
                low = 100.0,
                high = 200.0,
                close = 190.0,
                timeframe = Timeframe.H4
            ),
            h1 = candles(
                low = 100.0,
                high = 200.0,
                close = 190.0,
                timeframe = Timeframe.H1
            )
        )

        val result = LocationAnalyzer.analyze(snapshot)

        assertEquals(PriceLocation.PREMIUM, result.location)
        assertEquals(75, result.strength)
    }

    @Test
    fun price_near_middle_returns_equilibrium() {
        val snapshot = createSnapshot(
            h4 = candles(
                low = 100.0,
                high = 200.0,
                close = 150.0,
                timeframe = Timeframe.H4
            ),
            h1 = candles(
                low = 100.0,
                high = 200.0,
                close = 150.0,
                timeframe = Timeframe.H1
            )
        )

        val result = LocationAnalyzer.analyze(snapshot)

        assertEquals(PriceLocation.EQUILIBRIUM, result.location)
        assertEquals(50, result.strength)
    }

    @Test
    fun price_between_discount_and_equilibrium_returns_uncertain() {
        val snapshot = createSnapshot(
            h4 = candles(
                low = 100.0,
                high = 200.0,
                close = 130.0,
                timeframe = Timeframe.H4
            ),
            h1 = candles(
                low = 100.0,
                high = 200.0,
                close = 130.0,
                timeframe = Timeframe.H1
            )
        )

        val result = LocationAnalyzer.analyze(snapshot)

        assertEquals(PriceLocation.UNCERTAIN, result.location)
        assertEquals(20, result.strength)
    }

    @Test
    fun price_between_equilibrium_and_premium_returns_uncertain() {
        val snapshot = createSnapshot(
            h4 = candles(
                low = 100.0,
                high = 200.0,
                close = 170.0,
                timeframe = Timeframe.H4
            ),
            h1 = candles(
                low = 100.0,
                high = 200.0,
                close = 170.0,
                timeframe = Timeframe.H1
            )
        )

        val result = LocationAnalyzer.analyze(snapshot)

        assertEquals(PriceLocation.UNCERTAIN, result.location)
        assertEquals(20, result.strength)
    }

        private fun createSnapshot(
        h4: List<Candle>,
        h1: List<Candle>
    ): MarketSnapshot {
        return MarketSnapshot(
            symbol = "XAUUSD",
            capturedAt = System.currentTimeMillis(),
            h4 = h4,
            h1 = h1,
            m15 = emptyList(),
            m5 = emptyList(),
            m1 = emptyList()
        )
    }

    private fun candles(
        low: Double,
        high: Double,
        close: Double,
        timeframe: Timeframe
    ): List<Candle> {
        return listOf(
            Candle(
                timestamp = 1L,
                open = close,
                high = high,
                low = low,
                close = close,
                volume = 100.0,
                timeframe = timeframe
            )
        )
    }
}
