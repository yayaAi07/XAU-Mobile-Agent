package engine.data

class MarketDataRepository(
    private val source: MarketDataSource,
    private val cacheDurationMillis: Long = 60_000L
) {

    private var cachedSnapshot: MarketSnapshot? = null

    fun getSnapshot(
        symbol: String,
        nowMillis: Long
    ): MarketSnapshot {

        val cached = cachedSnapshot

        if (
            cached != null &&
            cached.symbol == symbol &&
            nowMillis - cached.capturedAt < cacheDurationMillis
        ) {
            return cached
        }

        val freshSnapshot =
            source.getSnapshot(
                symbol = symbol,
                nowMillis = nowMillis
            )

        cachedSnapshot = freshSnapshot

        return freshSnapshot
    }

    fun getEvidence(
        symbol: String,
        nowMillis: Long
    ): MarketDataEvidence {

        val snapshot =
            getSnapshot(
                symbol = symbol,
                nowMillis = nowMillis
            )

        return MarketDataEvidenceBuilder.build(
            snapshot = snapshot,
            nowMillis = nowMillis
        )
    }

    fun getValidatedEvidence(
        symbol: String,
        nowMillis: Long
    ): MarketDataValidationResult {

        val evidence =
            getEvidence(
                symbol = symbol,
                nowMillis = nowMillis
            )

        return MarketDataQualityValidator.validate(
            evidence
        )
    }

    fun clearCache() {
        cachedSnapshot = null
    }
}
