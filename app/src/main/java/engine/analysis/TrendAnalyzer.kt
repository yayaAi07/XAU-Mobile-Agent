package engine.analysis

import engine.data.Candle
import engine.data.MarketSnapshot

enum class TrendDirection {
    UP,
    DOWN,
    SIDEWAYS,
    UNCERTAIN
}

data class TrendResult(
    val direction: TrendDirection,
    val strength: Int
) {
    init {
        require(strength in 0..100) {
            "strength must be between 0 and 100"
        }
    }
}

object TrendAnalyzer {

    fun analyze(snapshot: MarketSnapshot): TrendResult {

        val h4Direction = directionOf(snapshot.h4)
        val h1Direction = directionOf(snapshot.h1)
        val m15Direction = directionOf(snapshot.m15)

        return when {
            // اتجاه صاعد متوافق
            h4Direction == TrendDirection.UP &&
                h1Direction == TrendDirection.UP &&
                m15Direction == TrendDirection.UP -> {

                TrendResult(TrendDirection.UP, 90)
            }

            // اتجاه هابط متوافق
            h4Direction == TrendDirection.DOWN &&
                h1Direction == TrendDirection.DOWN &&
                m15Direction == TrendDirection.DOWN -> {

                TrendResult(TrendDirection.DOWN, 90)
            }

            // اتجاه عام صاعد لكن الإطار الأصغر غير متوافق
            h4Direction == TrendDirection.UP &&
                h1Direction == TrendDirection.UP -> {

                TrendResult(TrendDirection.UP, 70)
            }

            // اتجاه عام هابط لكن الإطار الأصغر غير متوافق
            h4Direction == TrendDirection.DOWN &&
                h1Direction == TrendDirection.DOWN -> {

                TrendResult(TrendDirection.DOWN, 70)
            }

            // تعارض بين الإطارات
            h4Direction != h1Direction -> {

                TrendResult(TrendDirection.UNCERTAIN, 30)
            }

            // عرضي
            h4Direction == TrendDirection.SIDEWAYS ||
                h1Direction == TrendDirection.SIDEWAYS -> {

                TrendResult(TrendDirection.SIDEWAYS, 50)
            }

            else -> {
                TrendResult(TrendDirection.UNCERTAIN, 20)
            }
        }
    }

    private fun directionOf(candles: List<Candle>): TrendDirection {

        if (candles.size < 3) {
            return TrendDirection.UNCERTAIN
        }

        val first = candles.first().close
        val last = candles.last().close

        val change = last - first
        val threshold = first * 0.003 // 0.3%

        return when {
            change > threshold -> TrendDirection.UP
            change < -threshold -> TrendDirection.DOWN
            else -> TrendDirection.SIDEWAYS
        }
    }
}
