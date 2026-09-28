package engine.data

class MarketDataRepository(
    private val source: MarketDataSource,
    private val cacheDurationMillis: Long = 60_000L
) {

    private var cachedSnapshot: MarketSnapshot? = null

    fun getSnapshot(
        symbol: String,
        now: Long
    ): MarketSnapshot {

        val cached = cachedSnapshot

        if (
            cached != null &&
            cached.symbol == symbol &&
            now - cached.capturedAt < cacheDurationMillis
        ) {
            return cached
        }

        val freshSnapshot =
            source.getSnapshot(
                symbol = symbol,
                now = now
            )

        cachedSnapshot = freshSnapshot

        return freshSnapshot
    }

    fun getEvidence(
        symbol: String,
        now: Long
    ): MarketDataEvidence {

        val snapshot =
            getSnapshot(
                symbol = symbol,
                now = now
            )

        return MarketDataEvidenceBuilder.build(
            snapshot = snapshot,
            nowMillis = now
        )
    }

    fun getValidatedEvidence(
        symbol: String,
        now: Long
    ): MarketDataValidationResult {

        val evidence =
            getEvidence(
                symbol = symbol,
                now = now
            )

        return MarketDataQualityValidator.validate(
            evidence
        )
    }

    fun clearCache() {
        cachedSnapshot = null
    }
}
