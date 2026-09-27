package engine.data

enum class Timeframe {
    M1,
    M5,
    M15,
    H1,
    H4
}

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val timeframe: Timeframe
) {
    init {
        require(high >= low) {
            "high must be greater than or equal to low"
        }
        require(high >= open && high >= close) {
            "high must be the highest price"
        }
        require(low <= open && low <= close) {
            "low must be the lowest price"
        }
        require(volume >= 0.0) {
            "volume must be non-negative"
        }
    }
}
