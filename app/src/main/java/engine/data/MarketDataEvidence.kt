package engine.data

enum class MarketDataQuality {
    FRESH,
    STALE,
    INCOMPLETE,
    EMPTY
}

data class MarketDataEvidence(
    val symbol: String,
    val capturedAt: Long,
    val ageMillis: Long,
    val quality: MarketDataQuality,
    val totalCandles: Int,
    val availableTimeframes: Set<Timeframe>
)

object MarketDataEvidenceBuilder {

    private const val MAX_FRESH_AGE_MILLIS = 60_000L

    fun build(
        snapshot: MarketSnapshot,
        nowMillis: Long
    ): MarketDataEvidence {

        require(nowMillis >= 0L) {
            "nowMillis must not be negative"
        }

        val timeframeLists = listOf(
            Timeframe.H4 to snapshot.h4,
            Timeframe.H1 to snapshot.h1,
            Timeframe.M15 to snapshot.m15,
            Timeframe.M5 to snapshot.m5,
            Timeframe.M1 to snapshot.m1
        )

        val availableTimeframes =
            timeframeLists
                .filter { it.second.isNotEmpty() }
                .map { it.first }
                .toSet()

        val totalCandles =
            timeframeLists.sumOf { it.second.size }

        val ageMillis =
            (nowMillis - snapshot.capturedAt)
                .coerceAtLeast(0L)

        val quality = when {
            totalCandles == 0 ->
                MarketDataQuality.EMPTY

            ageMillis > MAX_FRESH_AGE_MILLIS ->
                MarketDataQuality.STALE

            availableTimeframes.size < Timeframe.entries.size ->
                MarketDataQuality.INCOMPLETE

            else ->
                MarketDataQuality.FRESH
        }

        return MarketDataEvidence(
            symbol = snapshot.symbol,
            capturedAt = snapshot.capturedAt,
            ageMillis = ageMillis,
            quality = quality,
            totalCandles = totalCandles,
            availableTimeframes = availableTimeframes
        )
    }
}
