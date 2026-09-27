package engine.analysis

import engine.data.Candle
import engine.data.MarketSnapshot

enum class ADType {
    BUY_AD,
    SELL_AD,
    NONE
}

data class ADResult(
    val type: ADType,
    val timeframe: String,
    val strength: Int
) {
    init {
        require(strength in 0..100) {
            "strength must be between 0 and 100"
        }
    }
}

object ADAnalyzer {

    private const val WICK_TOLERANCE = 0.05
    private const val MIN_WICK_RATIO = 0.30

    fun analyze(snapshot: MarketSnapshot): ADResult {

        val h1Result = analyzeLatest(snapshot.h1, "H1")

        if (h1Result.type != ADType.NONE) {
            return h1Result
        }

        val h4Result = analyzeLatest(snapshot.h4, "H4")

        if (h4Result.type != ADType.NONE) {
            return h4Result
        }

        return ADResult(
            type = ADType.NONE,
            timeframe = "NONE",
            strength = 0
        )
    }

    private fun analyzeLatest(
        candles: List<Candle>,
        timeframe: String
    ): ADResult {

        val candle = candles.lastOrNull()
            ?: return ADResult(
                type = ADType.NONE,
                timeframe = timeframe,
                strength = 0
            )

        return classify(candle, timeframe)
    }

    private fun classify(
        candle: Candle,
        timeframe: String
    ): ADResult {

        val range = candle.high - candle.low

        if (range <= 0.0) {
            return ADResult(
                type = ADType.NONE,
                timeframe = timeframe,
                strength = 0
            )
        }

        val bodyHigh = maxOf(
            candle.open,
            candle.close
        )

        val bodyLow = minOf(
            candle.open,
            candle.close
        )

        val upperWick =
            candle.high - bodyHigh

        val lowerWick =
            bodyLow - candle.low

        val upperWickRatio =
            upperWick / range

        val lowerWickRatio =
            lowerWick / range

        /*
         * BUY AD
         *
         * Flat top:
         * high is effectively equal to the body high.
         *
         * Therefore:
         * - no meaningful upper wick
         * - meaningful lower wick
         *
         * A candle with two meaningful wicks
         * cannot become BUY AD.
         */
        val isBuyAD =
            upperWickRatio <= WICK_TOLERANCE &&
            lowerWickRatio >= MIN_WICK_RATIO

        /*
         * SELL AD
         *
         * Flat bottom:
         * low is effectively equal to the body low.
         *
         * Therefore:
         * - no meaningful lower wick
         * - meaningful upper wick
         *
         * A candle with two meaningful wicks
         * cannot become SELL AD.
         */
        val isSellAD =
            lowerWickRatio <= WICK_TOLERANCE &&
            upperWickRatio >= MIN_WICK_RATIO

        return when {
            isBuyAD -> {

                val strength =
                    calculateStrength(
                        dominantWickRatio = lowerWickRatio,
                        flatWickRatio = upperWickRatio
                    )

                ADResult(
                    type = ADType.BUY_AD,
                    timeframe = timeframe,
                    strength = strength
                )
            }

            isSellAD -> {

                val strength =
                    calculateStrength(
                        dominantWickRatio = upperWickRatio,
                        flatWickRatio = lowerWickRatio
                    )

                ADResult(
                    type = ADType.SELL_AD,
                    timeframe = timeframe,
                    strength = strength
                )
            }

            else -> {

                ADResult(
                    type = ADType.NONE,
                    timeframe = timeframe,
                    strength = 0
                )
            }
        }
    }

    private fun calculateStrength(
        dominantWickRatio: Double,
        flatWickRatio: Double
    ): Int {

        val wickScore =
            ((dominantWickRatio - MIN_WICK_RATIO) /
                (1.0 - MIN_WICK_RATIO))
                .coerceIn(0.0, 1.0)

        val flatScore =
            (1.0 -
                (flatWickRatio / WICK_TOLERANCE))
                .coerceIn(0.0, 1.0)

        return (
            60.0 +
            wickScore * 25.0 +
            flatScore * 15.0
        ).toInt()
            .coerceIn(0, 100)
    }
}
