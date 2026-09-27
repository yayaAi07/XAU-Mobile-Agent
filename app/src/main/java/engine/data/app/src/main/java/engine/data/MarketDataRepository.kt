package engine.data

class MarketDataRepository(
    private val dataSource: MarketDataSource
) {
    private var cachedSnapshot: MarketSnapshot? = null
    private var cachedAt: Long = 0L

    fun getSnapshot(
        symbol: String,
        now: Long,
        maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS
    ): MarketSnapshot {

        val cached = cachedSnapshot

        if (
            cached != null &&
            cached.symbol == symbol &&
            (now - cachedAt) <= maxAgeMillis
        ) {
            return cached
        }

        val fresh = dataSource.getSnapshot(symbol, now)

        cachedSnapshot = fresh
        cachedAt = now

        return fresh
    }

    fun clearCache() {
        cachedSnapshot = null
        cachedAt = 0L
    }

    companion object {
        const val DEFAULT_MAX_AGE_MILLIS: Long = 60_000L
    }
}
