package engine.data

class MockMarketDataSource : MarketDataSource {

    override fun getSnapshot(
        symbol: String,
        now: Long
    ): MarketSnapshot {

        return MarketSnapshot(
            symbol = symbol,
            capturedAt = now,
            h4 = emptyList(),
            h1 = emptyList(),
            m15 = emptyList(),
            m5 = emptyList(),
            m1 = emptyList()
        )
    }
}
