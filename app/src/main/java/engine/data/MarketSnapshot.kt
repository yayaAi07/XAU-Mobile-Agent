
package engine.data

data class MarketSnapshot(
    val symbol: String,
    val capturedAt: Long,

    val h4: List<Candle>,
    val h1: List<Candle>,
    val m15: List<Candle>,
    val m5: List<Candle>,
    val m1: List<Candle>
) {
    init {
        require(symbol.isNotBlank()) {
            "symbol must not be blank"
        }

        require(capturedAt > 0L) {
            "capturedAt must be greater than 0"
        }

        require(h4.all { it.timeframe == Timeframe.H4 }) {
            "h4 must contain only H4 candles"
        }

        require(h1.all { it.timeframe == Timeframe.H1 }) {
            "h1 must contain only H1 candles"
        }

        require(m15.all { it.timeframe == Timeframe.M15 }) {
            "m15 must contain only M15 candles"
        }

        require(m5.all { it.timeframe == Timeframe.M5 }) {
            "m5 must contain only M5 candles"
        }

        require(m1.all { it.timeframe == Timeframe.M1 }) {
            "m1 must contain only M1 candles"
        }
    }

    fun latest(timeframe: Timeframe): Candle? =
        when (timeframe) {
            Timeframe.H4 -> h4.lastOrNull()
            Timeframe.H1 -> h1.lastOrNull()
            Timeframe.M15 -> m15.lastOrNull()
            Timeframe.M5 -> m5.lastOrNull()
            Timeframe.M1 -> m1.lastOrNull()
        }
}
