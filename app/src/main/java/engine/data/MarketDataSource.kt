package engine.data

interface MarketDataSource {

    fun getSnapshot(
        symbol: String,
        now: Long
    ): MarketSnapshot
}
