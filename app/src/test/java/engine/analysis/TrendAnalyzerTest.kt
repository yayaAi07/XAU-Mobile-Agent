package engine.analysis

import engine.data.Candle
import engine.data.MarketSnapshot
import engine.data.Timeframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendAnalyzerTest {

    @Test
    fun aligned_uptrend_returns_up_with_high_strength() {

        val snapshot = createSnapshot(
            h4Closes = listOf(2000.0, 2005.0, 2010.0),
            h1Closes = listOf(2000.0, 2005.0, 2010.0),
            m15Closes = listOf(2000.0, 2005.0, 2010.0)
        )

        val result =
            TrendAnalyzer.analyze(snapshot)

        assertEquals(
            TrendDirection.UP,
            result.direction
        )

        assertEquals(
            90,
            result.strength
        )
    }

    @Test
    fun aligned_downtrend_returns_down_with_high_strength() {

        val snapshot = createSnapshot(
            h4Closes = listOf(2010.0, 2005.0, 2000.0),
            h1Closes = listOf(2010.0, 2005.0, 2000.0),
            m15Closes = listOf(2010.0, 2005.0, 2000.0)
        )

        val result =
            TrendAnalyzer.analyze(snapshot)

        assertEquals(
            TrendDirection.DOWN,
            result.direction
        )

        assertEquals(
            90,
            result.strength
        )
    }

    @Test
    fun h4_and_h1_uptrend_returns_up_even_if_m15_not_aligned() {

        val snapshot = createSnapshot(
            h4Closes = listOf(2000.0, 2005.0, 2010.0),
            h1Closes = listOf(2000.0, 2005.0, 2010.0),
            m15Closes = listOf(2000.0, 2001.0, 2000.5)
        )

        val result =
            TrendAnalyzer.analyze(snapshot)

        assertEquals(
            TrendDirection.UP,
            result.direction
        )

        assertEquals(
            70,
            result.strength
        )
    }

    @Test
    fun h4_and_h1_downtrend_returns_down_even_if_m15_not_aligned() {

        val snapshot = createSnapshot(
            h4Closes = listOf(2010.0, 2005.0, 2000.0),
            h1Closes = listOf(2010.0, 2005.0, 2000.0),
            m15Closes = listOf(2000.0, 1999.0, 1999.5)
        )

        val result =
            TrendAnalyzer.analyze(snapshot)

        assertEquals(
            TrendDirection.DOWN,
            result.direction
        )

        assertEquals(
            70,
            result.strength
        )
    }

    @Test
    fun conflicting_h4_and_h1_returns_uncertain() {

        val snapshot = createSnapshot(
            h4Closes = listOf(2000.0, 2005.0, 2010.0),
            h1Closes = listOf(2010.0, 2005.0, 2000.0),
            m15Closes = listOf(2000.0, 2001.0, 2002.0)
        )

        val result =
            TrendAnalyzer.analyze(snapshot)

        assertEquals(
            TrendDirection.UNCERTAIN,
            result.direction
        )

        assertEquals(
            30,
            result.strength
        )
    }

    @Test
    fun sideways_h4_returns_sideways_when_no_higher_timeframe_conflict() {

        val snapshot = createSnapshot(
            h4Closes = listOf(2000.0, 2001.0, 2000.5),
            h1Closes = listOf(2000.0, 2000.5, 2000.2),
            m15Closes = listOf(2000.0, 2000.3, 2000.1)
        )

        val result =
            TrendAnalyzer.analyze(snapshot)

        assertEquals(
            TrendDirection.SIDEWAYS,
            result.direction
        )

        assertEquals(
            50,
            result.strength
        )
    }

    @Test
    fun insufficient_candles_produce_uncertain_result() {

        val snapshot = createSnapshot(
            h4Closes = listOf(2000.0, 2005.0),
            h1Closes = listOf(2000.0, 2005.0),
            m15Closes = listOf(2000.0, 2005.0)
        )

        val result =
            TrendAnalyzer.analyze(snapshot)

        assertEquals(
            TrendDirection.UNCERTAIN,
            result.direction
        )

        assertTrue(
            result.strength in 0..100
        )
    }

    private fun createSnapshot(
        h4Closes: List<Double>,
        h1Closes: List<Double>,
        m15Closes: List<Double>
    ): MarketSnapshot {

        return MarketSnapshot(
            symbol = "XAUUSD",
            capturedAt = System.currentTimeMillis(),
            h4 = createCandles(
                h4Closes,
                Timeframe.H4
            ),
            h1 = createCandles(
                h1Closes,
                Timeframe.H1
            ),
            m15 = createCandles(
                m15Closes,
                Timeframe.M15
            ),
            m5 = emptyList(),
            m1 = emptyList()
        )
    }

    private fun createCandles(
        closes: List<Double>,
        timeframe: Timeframe
    ): List<Candle> {

        return closes.mapIndexed { index, close ->

            Candle(
                timestamp = index.toLong(),
                open = close,
                high = close,
                low = close,
                close = close,
                volume = 100.0,
                timeframe = timeframe
            )
        }
    }
}
