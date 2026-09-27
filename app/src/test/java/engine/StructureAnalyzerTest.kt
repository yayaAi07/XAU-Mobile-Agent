package engine.analysis

import engine.data.Candle
import engine.data.MarketSnapshot
import engine.data.Timeframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StructureAnalyzerTest {

    @Test
    fun insufficient_candles_returns_uncertain() {

        val snapshot = createSnapshot(
            h1 = createCandles(
                highs = listOf(100.0, 101.0, 102.0, 103.0),
                lows = listOf(98.0, 99.0, 100.0, 101.0),
                closes = listOf(99.0, 100.0, 101.0, 102.0),
                timeframe = Timeframe.H1
            ),
            m15 = createCandles(
                highs = listOf(100.0, 101.0, 102.0, 103.0),
                lows = listOf(98.0, 99.0, 100.0, 101.0),
                closes = listOf(99.0, 100.0, 101.0, 102.0),
                timeframe = Timeframe.M15
            )
        )

        val result =
            StructureAnalyzer.analyze(snapshot)

        assertEquals(
            StructureDirection.UNCERTAIN,
            result.direction
        )

        assertFalse(result.hasBreakOfStructure)
        assertFalse(result.hasChangeOfCharacter)
        assertEquals(0, result.strength)
    }

    @Test
    fun bullish_structure_is_detected_when_higher_high_and_higher_low_exist() {

        val h1 = bullishCandles(
            lastClose = 108.0
        )

        val m15 = bullishCandles(
            lastClose = 108.0
        )

        val snapshot =
            createSnapshot(
                h1 = h1,
                m15 = m15
            )

        val result =
            StructureAnalyzer.analyze(snapshot)

        assertEquals(
            StructureDirection.BULLISH,
            result.direction
        )

        assertFalse(result.hasBreakOfStructure)
        assertFalse(result.hasChangeOfCharacter)

        assertEquals(
            55,
            result.strength
        )
    }

    @Test
    fun bearish_structure_is_detected_when_lower_high_and_lower_low_exist() {

        val h1 = bearishCandles(
            lastClose = 97.0
        )

        val m15 = bearishCandles(
            lastClose = 97.0
        )

        val snapshot =
            createSnapshot(
                h1 = h1,
                m15 = m15
            )

        val result =
            StructureAnalyzer.analyze(snapshot)

        assertEquals(
            StructureDirection.BEARISH,
            result.direction
        )

        assertFalse(result.hasBreakOfStructure)
        assertFalse(result.hasChangeOfCharacter)

        assertEquals(
            55,
            result.strength
        )
    }

    @Test
    fun bullish_break_of_structure_is_detected() {

        val h1 = bullishCandles(
            lastClose = 111.0
        )

        val m15 = bullishCandles(
            lastClose = 111.0
        )

        val snapshot =
            createSnapshot(
                h1 = h1,
                m15 = m15
            )

        val result =
            StructureAnalyzer.analyze(snapshot)

        assertEquals(
            StructureDirection.BULLISH,
            result.direction
        )

        assertTrue(result.hasBreakOfStructure)
        assertFalse(result.hasChangeOfCharacter)

        assertEquals(
            70,
            result.strength
        )
    }

    @Test
    fun bearish_structure_with_bullish_break_is_change_of_character() {

        val h1 = bearishCandles(
            lastClose = 106.0
        )

        val m15 = bearishCandles(
            lastClose = 106.0
        )

        val snapshot =
            createSnapshot(
                h1 = h1,
                m15 = m15
            )

        val result =
            StructureAnalyzer.analyze(snapshot)

        assertEquals(
            StructureDirection.BEARISH,
            result.direction
        )

        assertTrue(result.hasBreakOfStructure)
        assertTrue(result.hasChangeOfCharacter)

        assertEquals(
            80,
            result.strength
        )
    }

    @Test
    fun bullish_structure_with_bearish_break_is_change_of_character() {

        val h1 = bullishCandles(
            lastClose = 94.0
        )

        val m15 = bullishCandles(
            lastClose = 94.0
        )

        val snapshot =
            createSnapshot(
                h1 = h1,
                m15 = m15
            )

        val result =
            StructureAnalyzer.analyze(snapshot)

        assertEquals(
            StructureDirection.BULLISH,
            result.direction
        )

        assertTrue(result.hasBreakOfStructure)
        assertTrue(result.hasChangeOfCharacter)

        assertEquals(
            80,
            result.strength
        )
    }

    @Test
    fun conflicting_h1_and_m15_structure_returns_uncertain() {

        val snapshot =
            createSnapshot(
                h1 = bullishCandles(
                    lastClose = 108.0
                ),
                m15 = bearishCandles(
                    lastClose = 97.0
                )
            )

        val result =
            StructureAnalyzer.analyze(snapshot)

        assertEquals(
            StructureDirection.UNCERTAIN,
            result.direction
        )

        assertEquals(
            30,
            result.strength
        )
    }

    private fun createSnapshot(
        h1: List<Candle>,
        m15: List<Candle>
    ): MarketSnapshot {

        return MarketSnapshot(
            symbol = "XAUUSD",
            capturedAt = System.currentTimeMillis(),
            h4 = emptyList(),
            h1 = h1,
            m15 = m15,
            m5 = emptyList(),
            m1 = emptyList()
        )
    }

    private fun bullishCandles(
        lastClose: Double
    ): List<Candle> {

        val highs = listOf(
            100.0,
            101.0,
            105.0,
            101.0,
            102.0,
            106.0,
            110.0,
            106.0,
            107.0,
            108.0
        )

        val lows = listOf(
            98.0,
            99.0,
            100.0,
            96.0,
            95.0,
            97.0,
            99.0,
            98.0,
            100.0,
            101.0
        )

        val closes = List(10) { index ->
            if (index == 9) {
                lastClose
            } else {
                (highs[index] + lows[index]) / 2.0
            }
        }

        return createCandles(
            highs = highs,
            lows = lows,
            closes = closes,
            timeframe = Timeframe.H1
        )
    }

    private fun bearishCandles(
        lastClose: Double
    ): List<Candle> {

        val highs = listOf(
            112.0,
            111.0,
            110.0,
            111.0,
            109.0,
            108.0,
            105.0,
            107.0,
            106.0,
            104.0
        )

        val lows = listOf(
            108.0,
            107.0,
            106.0,
            104.0,
            100.0,
            103.0,
            102.0,
            101.0,
            95.0,
            96.0
        )

        val closes = List(10) { index ->
            if (index == 9) {
                lastClose
            } else {
                (highs[index] + lows[index]) / 2.0
            }
        }

        return createCandles(
            highs = highs,
            lows = lows,
            closes = closes,
            timeframe = Timeframe.H1
        )
    }

    private fun createCandles(
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        timeframe: Timeframe
    ): List<Candle> {

        return highs.indices.map { index ->

            val close = closes[index]

            Candle(
                timestamp = index.toLong(),
                open = close,
                high = highs[index],
                low = lows[index],
                close = close,
                volume = 100.0,
                timeframe = timeframe
            )
        }
    }
}
