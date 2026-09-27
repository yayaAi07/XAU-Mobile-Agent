package engine.analysis

import engine.data.MarketSnapshot

data class MarketAnalysisResult(
    val marketState: MarketState,
    val trend: TrendResult,
    val structure: StructureResult,
    val location: LocationResult,
    val ad: ADResult,
    val overallStrength: Int
) {
    init {
        require(overallStrength in 0..100) {
            "overallStrength must be between 0 and 100"
        }
    }
}

object MarketAnalysisEngine {

    fun analyze(snapshot: MarketSnapshot): MarketAnalysisResult {

        val trend = TrendAnalyzer.analyze(snapshot)

        val structure = StructureAnalyzer.analyze(snapshot)

        val location = LocationAnalyzer.analyze(snapshot)

        val ad = ADAnalyzer.analyze(snapshot)

        val marketState = resolveMarketState(
            trend = trend,
            structure = structure
        )

        val overallStrength = resolveOverallStrength(
            trend = trend,
            structure = structure,
            location = location,
            ad = ad
        )

        return MarketAnalysisResult(
            marketState = marketState,
            trend = trend,
            structure = structure,
            location = location,
            ad = ad,
            overallStrength = overallStrength
        )
    }

    private fun resolveMarketState(
        trend: TrendResult,
        structure: StructureResult
    ): MarketState {

        return when {
            trend.direction == TrendDirection.UP &&
                structure.direction == StructureDirection.BULLISH -> {

                MarketState.TREND_UP
            }

            trend.direction == TrendDirection.DOWN &&
                structure.direction == StructureDirection.BEARISH -> {

                MarketState.TREND_DOWN
            }

            trend.direction == TrendDirection.SIDEWAYS -> {
                MarketState.RANGE
            }

            trend.direction == TrendDirection.UNCERTAIN ||
                structure.direction == StructureDirection.UNCERTAIN -> {

                MarketState.UNCERTAIN
            }

            else -> MarketState.RANGE
        }
    }

    private fun resolveOverallStrength(
        trend: TrendResult,
        structure: StructureResult,
        location: LocationResult,
        ad: ADResult
    ): Int {

        val weighted =
            trend.strength * 0.30 +
            structure.strength * 0.30 +
            location.strength * 0.20 +
            ad.strength * 0.20

        return weighted.toInt().coerceIn(0, 100)
    }
}
