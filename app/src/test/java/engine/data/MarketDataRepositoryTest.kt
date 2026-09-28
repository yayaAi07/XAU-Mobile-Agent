package engine.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class MarketDataRepositoryTest {

    @Test
    fun firstRequest_loadsDataFromSource() {
        val source = FakeMarketDataSource()
        val repository = MarketDataRepository(
            source = source,
            cacheDurationMillis = 60_000L
        )

        val snapshot = repository.getSnapshot(
            symbol = "XAUUSD",
            now = 1_000L
        )

        assertEquals("XAUUSD", snapshot.symbol)
        assertEquals(1_000L, snapshot.capturedAt)
        assertEquals(1, source.requestCount)
    }

    @Test
    fun requestWithinCacheDuration_returnsCachedSnapshot() {
        val source = FakeMarketDataSource()
        val repository = MarketDataRepository(
            source = source,
            cacheDurationMillis = 60_000L
        )

        val first = repository.getSnapshot(
            symbol = "XAUUSD",
            now = 1_000L
        )

        val second = repository.getSnapshot(
            symbol = "XAUUSD",
            now = 30_000L
        )

        assertSame(first, second)
        assertEquals(1, source.requestCount)
    }

    @Test
    fun requestAfterCacheExpires_loadsFreshSnapshot() {
        val source = FakeMarketDataSource()
        val repository = MarketDataRepository(
            source = source,
            cacheDurationMillis = 60_000L
        )

        val first = repository.getSnapshot(
            symbol = "XAUUSD",
            now = 1_000L
        )

        val second = repository.getSnapshot(
            symbol = "XAUUSD",
            now = 61_001L
        )

        assertNotSame(first, second)
        assertEquals(2, source.requestCount)
        assertEquals(61_001L, second.capturedAt)
    }

    @Test
    fun differentSymbol_doesNotReuseCachedSnapshot() {
        val source = FakeMarketDataSource()
        val repository = MarketDataRepository(
            source = source,
            cacheDurationMillis = 60_000L
        )

        val first = repository.getSnapshot(
            symbol = "XAUUSD",
            now = 1_000L
        )

        val second = repository.getSnapshot(
            symbol = "BTCUSD",
            now = 2_000L
        )

        assertNotSame(first, second)
        assertEquals("XAUUSD", first.symbol)
        assertEquals("BTCUSD", second.symbol)
        assertEquals(2, source.requestCount)
    }

    @Test
    fun clearCache_forcesFreshRequest() {
        val source = FakeMarketDataSource()
        val repository = MarketDataRepository(
            source = source,
            cacheDurationMillis = 60_000L
        )

        val first = repository.getSnapshot(
            symbol = "XAUUSD",
            now = 1_000L
        )

        repository.clearCache()

        val second = repository.getSnapshot(
            symbol = "XAUUSD",
            now = 2_000L
        )

        assertNotSame(first, second)
        assertEquals(2, source.requestCount)
    }

    @Test
    fun getEvidence_returnsEvidenceForRequestedSymbol() {
        val source = FakeMarketDataSource()
        val repository = MarketDataRepository(
            source = source,
            cacheDurationMillis = 60_000L
        )

        val evidence = repository.getEvidence(
            symbol = "XAUUSD",
            now = 2_000L
        )

        assertEquals("XAUUSD", evidence.symbol)
    }

    private class FakeMarketDataSource : MarketDataSource {

        var requestCount = 0

        override fun getSnapshot(
            symbol: String,
            now: Long
        ): MarketSnapshot {

            requestCount++

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
}
