package engine.analysis

import engine.data.Candle
import engine.data.MarketSnapshot
import engine.data.Timeframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ADAnalyzerTest {

    @Test
    fun buy_ad_with_single_lower_wick_is_detected() {

        val snapshot = createSnapshot(
            h1Candle = candle(
                open = 100.0,
                high = 105.0,
                low = 95.0,
                close = 105.0,
                timeframe = Timeframe.H1
            )
        )

        val result =
            ADAnalyzer.analyze(snapshot)

        assertEquals(
            ADType.BUY_AD,
            result.type
        )

        assertEquals(
            "H1",
            result.timeframe
        )

        assertTrue(
            result.strength in 0..100
        )
    }

    @Test
    fun sell_ad_with_single_upper_wick_is_detected() {

        val snapshot = createSnapshot(
            h1Candle = candle(
                open = 105.0,
                high = 110.0,
                low = 100.0,
                close = 100.0,
                timeframe = Timeframe.H1
            )
        )

        val result =
            ADAnalyzer.analyze(snapshot)

        assertEquals(
            ADType.SELL_AD,
            result.type
        )

        assertEquals(
            "H1",
            result.timeframe
        )

        assertTrue(
            result.strength in 0..100
        )
    }

    @Test
    fun candle_with_two_meaningful_wicks_is_not_ad() {

        val snapshot = createSnapshot(
            h1Candle = candle(
                open = 100.0,
                high = 107.0,
                low = 97.0,
                close = 105.0,
                timeframe = Timeframe.H1
            )
        )

        val result =
            ADAnalyzer.analyze(snapshot)

        assertEquals(
            ADType.NONE,
            result.type
        )

        assertEquals(
            "NONE",
            result.timeframe
        )

        assertEquals(
            0,
            result.strength
        )
    }

    @Test
    fun h1_ad_has_priority_over_h4_ad() {

        val snapshot = createSnapshot(
            h1Candle = candle(
                open = 100.0,
                high = 105.0,
                low = 95.0,
                close = 105.0,
                timeframe = Timeframe.H1
            ),
            h4Candle = candle(
                open = 105.0,
                high = 110.0,
                low = 100.0,
                close = 100.0,
                timeframe = Timeframe.H4
            )
        )

        val result =
            ADAnalyzer.analyze(snapshot)

        assertEquals(
            ADType.BUY_AD,
            result.type
        )

        assertEquals(
            "H1",
            result.timeframe
        )
    }

    @Test
    fun h4_ad_is_used_when_h1_has_no_ad() {

        val snapshot = createSnapshot(
            h1Candle = candle(
                open = 100.0,
                high = 107.0,
                low = 97.0,
                close = 105.0,
                timeframe = Timeframe.H1
            ),
            h4Candle = candle(
                open = 105.0,
                high = 110.0,
                low = 100.0,
                close = 100.0,
                timeframe = Timeframe.H4
            )
        )

        val result =
            ADAnalyzer.analyze(snapshot)

        assertEquals(
            ADType.SELL_AD,
            result.type
        )

        assertEquals(
            "H4",
            result.timeframe
        )
    }

    @Test
    fun no_candles_returns_none() {

        val snapshot =
            MarketSnapshot(
                symbol = "XAUUSD",
                capturedAt = System.currentTimeMillis(),
                h4 = emptyList(),
                h1 = emptyList(),
                m15 = emptyList(),
                m5 = emptyList(),
                m1 = emptyList()
            )

        val result =
            ADAnalyzer.analyze(snapshot)

        assertEquals(
            ADType.NONE,
            result.type
        )

        assertEquals(
            "NONE",
            result.timeframe
        )

        assertEquals(
            0,
            result.strength
        )
    }

    @Test
    fun zero_range_candle_is_not_ad() {

        val snapshot = createSnapshot(
            h1Candle = candle(
                open = 100.0,
                high = 100.0,
                low = 100.0,
                close = 100.0,
                timeframe = Timeframe.H1
            )
        )

        val result =
            ADAnalyzer.analyze(snapshot)

        assertEquals(
            ADType.NONE,
            result.type
        )

        assertEquals(
            0,
            result.strength
        )
    }

    private fun createSnapshot(
        h1Candle: Candle? = null,
        h4Candle: Candle? = null
    ): MarketSnapshot {

        return MarketSnapshot(
            symbol = "XAUUSD",
            capturedAt = System.currentTimeMillis(),

            h4 = h4Candle?.let {
                listOf(it)
            } ?: emptyList(),

            h1 = h1Candle?.let {
                listOf(it)
            } ?: emptyList(),

            m15 = emptyList(),
            m5 = emptyList(),
            m1 = emptyList()
        )
    }

    private fun candle(
        open: Double,
        high: Double,
        low: Double,
        close: Double,
        timeframe: Timeframe
    ): Candle {

        return Candle(
            timestamp = System.currentTimeMillis(),
            open = open,
            high = high,
            low = low,
            close = close,
            volume = 100.0,
            timeframe = timeframe
        )
    }
}
