package engine.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockMarketDataSourceTest {

    @Test
    fun getSnapshot_returnsRequestedSymbol() {
        val source = MockMarketDataSource()

        val snapshot = source.getSnapshot(
            symbol = "XAUUSD",
            now = 1_000L
        )

        assertEquals("XAUUSD", snapshot.symbol)
        assertEquals(1_000L, snapshot.capturedAt)
    }

    @Test
    fun getSnapshot_returnsEmptyTimeframes() {
        val source = MockMarketDataSource()

        val snapshot = source.getSnapshot(
            symbol = "XAUUSD",
            now = 1_000L
        )

        assertTrue(snapshot.h4.isEmpty())
        assertTrue(snapshot.h1.isEmpty())
        assertTrue(snapshot.m15.isEmpty())
        assertTrue(snapshot.m5.isEmpty())
        assertTrue(snapshot.m1.isEmpty())
    }
}
