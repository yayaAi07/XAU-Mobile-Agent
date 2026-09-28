package engine.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataQualityValidatorTest {

    @Test
    fun freshCompleteEvidence_isValid() {
        val evidence = evidence(
            quality = MarketDataQuality.FRESH,
            totalCandles = 5,
            timeframes = Timeframe.entries.toSet()
        )

        val result =
            MarketDataQualityValidator.validate(evidence)

        assertTrue(result.isValid)
        assertEquals(MarketDataQuality.FRESH, result.quality)
        assertTrue(result.reasons.isEmpty())
    }

    @Test
    fun staleEvidence_isInvalid() {
        val evidence = evidence(
            quality = MarketDataQuality.STALE,
            totalCandles = 5,
            timeframes = Timeframe.entries.toSet()
        )

        val result =
            MarketDataQualityValidator.validate(evidence)

        assertFalse(result.isValid)
        assertEquals(MarketDataQuality.STALE, result.quality)
        assertTrue(
            result.reasons.contains("Market data is stale")
        )
    }

    @Test
    fun incompleteEvidence_isInvalid() {
        val evidence = evidence(
            quality = MarketDataQuality.INCOMPLETE,
            totalCandles = 4,
            timeframes = setOf(
                Timeframe.H4,
                Timeframe.H1,
                Timeframe.M15,
                Timeframe.M5
            )
        )

        val result =
            MarketDataQualityValidator.validate(evidence)

        assertFalse(result.isValid)
        assertEquals(
            MarketDataQuality.INCOMPLETE,
            result.quality
        )
        assertTrue(
            result.reasons.contains("Market data is incomplete")
        )
    }

    @Test
    fun emptyEvidence_isInvalid() {
        val evidence = evidence(
            quality = MarketDataQuality.EMPTY,
            totalCandles = 0,
            timeframes = emptySet()
        )

        val result =
            MarketDataQualityValidator.validate(evidence)

        assertFalse(result.isValid)
        assertEquals(
            MarketDataQuality.EMPTY,
            result.quality
        )
        assertTrue(
            result.reasons.contains("Market data is empty")
        )
    }

    @Test
    fun blankSymbol_isInvalid() {
        val evidence = MarketDataEvidence(
            symbol = "",
            capturedAt = 1_000L,
            ageMillis = 1_000L,
            quality = MarketDataQuality.FRESH,
            totalCandles = 5,
            availableTimeframes = Timeframe.entries.toSet()
        )

        val result =
            MarketDataQualityValidator.validate(evidence)

        assertFalse(result.isValid)
        assertTrue(
            result.reasons.contains("Symbol is empty")
        )
    }

    private fun evidence(
        quality: MarketDataQuality,
        totalCandles: Int,
        timeframes: Set<Timeframe>
    ): MarketDataEvidence {
        return MarketDataEvidence(
            symbol = "XAUUSD",
            capturedAt = 1_000L,
            ageMillis = 1_000L,
            quality = quality,
            totalCandles = totalCandles,
            availableTimeframes = timeframes
        )
    }
}
