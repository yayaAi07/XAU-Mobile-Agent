package engine.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataEvidenceTest {

    @Test
    fun freshCompleteData_isDetectedCorrectly() {
        val capturedAt = 1_000L

        val snapshot = createSnapshot(
            capturedAt = capturedAt,
            h4 = listOf(candle(Timeframe.H4)),
            h1 = listOf(candle(Timeframe.H1)),
            m15 = listOf(candle(Timeframe.M15)),
            m5 = listOf(candle(Timeframe.M5)),
            m1 = listOf(candle(Timeframe.M1))
        )

        val evidence = MarketDataEvidenceBuilder.build(
            snapshot = snapshot,
            nowMillis = 61_000L
        )

        assertEquals(MarketDataQuality.FRESH, evidence.quality)
        assertEquals(5, evidence.totalCandles)
        assertEquals(5, evidence.availableTimeframes.size)
        assertEquals(60_000L, evidence.ageMillis)
    }

    @Test
    fun staleData_isDetectedCorrectly() {
        val capturedAt = 1_000L

        val snapshot = createSnapshot(
            capturedAt = capturedAt,
            h4 = listOf(candle(Timeframe.H4)),
            h1 = listOf(candle(Timeframe.H1)),
            m15 = listOf(candle(Timeframe.M15)),
            m5 = listOf(candle(Timeframe.M5)),
            m1 = listOf(candle(Timeframe.M1))
        )

        val evidence = MarketDataEvidenceBuilder.build(
            snapshot = snapshot,
            nowMillis = 61_001L + capturedAt
        )

        assertEquals(MarketDataQuality.STALE, evidence.quality)
        assertTrue(evidence.ageMillis > 60_000L)
    }

    @Test
    fun incompleteData_isDetectedCorrectly() {
        val snapshot = createSnapshot(
            capturedAt = 1_000L,
            h4 = listOf(candle(Timeframe.H4)),
            h1 = listOf(candle(Timeframe.H1)),
            m15 = listOf(candle(Timeframe.M15)),
            m5 = emptyList(),
            m1 = listOf(candle(Timeframe.M1))
        )

        val evidence = MarketDataEvidenceBuilder.build(
            snapshot = snapshot,
            nowMillis = 2_000L
        )

        assertEquals(MarketDataQuality.INCOMPLETE, evidence.quality)
        assertEquals(4, evidence.totalCandles)
        assertEquals(4, evidence.availableTimeframes.size)
    }

    @Test
    fun emptyData_isDetectedCorrectly() {
        val snapshot = createSnapshot(
            capturedAt = 1_000L,
            h4 = emptyList(),
            h1 = emptyList(),
            m15 = emptyList(),
            m5 = emptyList(),
            m1 = emptyList()
        )

        val evidence = MarketDataEvidenceBuilder.build(
            snapshot = snapshot,
            nowMillis = 2_000L
        )

        assertEquals(MarketDataQuality.EMPTY, evidence.quality)
        assertEquals(0, evidence.totalCandles)
        assertTrue(evidence.availableTimeframes.isEmpty())
    }

    private fun createSnapshot(
        capturedAt: Long,
        h4: List<Candle>,
        h1: List<Candle>,
        m15: List<Candle>,
        m5: List<Candle>,
        m1: List<Candle>
    ): MarketSnapshot {
        return MarketSnapshot(
            symbol = "XAUUSD",
            capturedAt = capturedAt,
            h4 = h4,
            h1 = h1,
            m15 = m15,
            m5 = m5,
            m1 = m1
        )
    }

    private fun candle(timeframe: Timeframe): Candle {
        return Candle(
            timestamp = 1L,
            open = 2000.0,
            high = 2010.0,
            low = 1990.0,
            close = 2005.0,
            volume = 100.0,
            timeframe = timeframe
        )
    }
}
