package engine.data

data class MarketDataValidationResult(
    val isValid: Boolean,
    val quality: MarketDataQuality,
    val reasons: List<String>
)

object MarketDataQualityValidator {

    fun validate(
        evidence: MarketDataEvidence
    ): MarketDataValidationResult {

        val reasons = mutableListOf<String>()

        if (evidence.symbol.isBlank()) {
            reasons.add("Symbol is empty")
        }

        if (evidence.capturedAt <= 0L) {
            reasons.add("Invalid capture timestamp")
        }

        if (evidence.ageMillis < 0L) {
            reasons.add("Invalid data age")
        }

        if (evidence.totalCandles < 0) {
            reasons.add("Invalid candle count")
        }

        when (evidence.quality) {

            MarketDataQuality.FRESH -> {
                if (evidence.totalCandles == 0) {
                    reasons.add("Fresh data contains no candles")
                }

                if (
                    evidence.availableTimeframes.size !=
                    Timeframe.entries.size
                ) {
                    reasons.add("Fresh data is missing timeframes")
                }
            }

            MarketDataQuality.STALE -> {
                reasons.add("Market data is stale")
            }

            MarketDataQuality.INCOMPLETE -> {
                reasons.add("Market data is incomplete")
            }

            MarketDataQuality.EMPTY -> {
                reasons.add("Market data is empty")
            }
        }

        return MarketDataValidationResult(
            isValid = reasons.isEmpty(),
            quality = evidence.quality,
            reasons = reasons
        )
    }
}
