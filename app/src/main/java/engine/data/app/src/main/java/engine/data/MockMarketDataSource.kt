package engine.data

import kotlin.random.Random

class MockMarketDataSource(
    private val random: Random = Random(42)
) : MarketDataSource {

    override fun getSnapshot(
        symbol: String,
        now: Long
    ): MarketSnapshot {
        require(symbol.isNotBlank()) {
            "symbol must not be blank"
        }

        return MarketSnapshot(
            symbol = symbol,
            capturedAt = now,
            h4 = generateCandles(Timeframe.H4, now, 50),
            h1 = generateCandles(Timeframe.H1, now, 50),
            m15 = generateCandles(Timeframe.M15, now, 50),
            m5 = generateCandles(Timeframe.M5, now, 50),
            m1 = generateCandles(Timeframe.M1, now, 50)
        )
    }

    private fun generateCandles(
        timeframe: Timeframe,
        now: Long,
        count: Int
    ): List<Candle> {

        val interval = intervalMillis(timeframe)

        var price = 2000.0

        return (0 until count).map { index ->

            val timestamp =
                now - (count - index) * interval

            val open = price

            val change =
                (random.nextDouble() - 0.5) * 5.0

            val close = open + change

            val high =
                maxOf(open, close) +
                    random.nextDouble() * 2.0

            val low =
                minOf(open, close) -
                    random.nextDouble() * 2.0

            price = close

            Candle(
                timestamp = timestamp,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = 100.0 + random.nextDouble() * 900.0,
                timeframe = timeframe
            )
        }
    }

    private fun intervalMillis(timeframe: Timeframe): Long =
        when (timeframe) {
            Timeframe.H4 -> 4 * 60 * 60 * 1000L
            Timeframe.H1 -> 60 * 60 * 1000L
            Timeframe.M15 -> 15 * 60 * 1000L
            Timeframe.M5 -> 5 * 60 * 1000L
            Timeframe.M1 -> 60 * 1000L
        }
}
