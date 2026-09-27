package engine.setup

import engine.analysis.ADResult
import engine.analysis.ADType
import engine.analysis.LocationResult
import engine.analysis.MarketAnalysisResult
import engine.analysis.MarketState
import engine.analysis.PriceLocation
import engine.analysis.StructureDirection
import engine.analysis.TrendDirection
import engine.safety.SafetyGate

enum class SetupDirection {
    BUY,
    SELL,
    NONE
}

data class Setup(
    val direction: SetupDirection,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val confidence: Int,
    val reasons: List<String>
) {
    init {
        require(confidence in 0..100) {
            "confidence must be between 0 and 100"
        }

        if (direction == SetupDirection.BUY) {
            require(stopLoss < entryPrice) {
                "For BUY, stopLoss must be below entryPrice"
            }
            require(takeProfit > entryPrice) {
                "For BUY, takeProfit must be above entryPrice"
            }
        }

        if (direction == SetupDirection.SELL) {
            require(stopLoss > entryPrice) {
                "For SELL, stopLoss must be above entryPrice"
            }
            require(takeProfit < entryPrice) {
                "For SELL, takeProfit must be below entryPrice"
            }
        }
    }
}

object SetupEngine {

    fun build(
        analysis: MarketAnalysisResult,
        entryPrice: Double
    ): Setup {

        val reasons = mutableListOf<String>()

        val direction = resolveDirection(
            analysis = analysis,
            reasons = reasons
        )

        if (direction == SetupDirection.NONE) {
            return Setup(
                direction = SetupDirection.NONE,
                entryPrice = entryPrice,
                stopLoss = entryPrice,
                takeProfit = entryPrice,
                confidence = 0,
                reasons = reasons
            )
        }

        val range = resolveRange(
            entryPrice = entryPrice,
            analysis = analysis
        )

        val stopLoss: Double
        val takeProfit: Double

        if (direction == SetupDirection.BUY) {
            stopLoss = entryPrice - range
            takeProfit = entryPrice + range * 2.0
        } else {
            stopLoss = entryPrice + range
            takeProfit = entryPrice - range * 2.0
        }

        val confidence = resolveConfidence(analysis)

        val risk = kotlin.math.abs(entryPrice - stopLoss)
        val reward = kotlin.math.abs(takeProfit - entryPrice)

        val riskRewardRatio =
            if (risk > 0.0) {
                reward / risk
            } else {
                Double.NaN
            }

        val safetyResult = SafetyGate.check(
            hasValidSetup = true,
            hasValidEntry = entryPrice.isFinite() && entryPrice > 0.0,
            hasValidStop = stopLoss.isFinite() && stopLoss > 0.0,
            hasValidTargets = takeProfit.isFinite() && takeProfit > 0.0,
            riskRewardRatio = riskRewardRatio
        )

        if (!safetyResult.approved) {

            reasons.add(
                "Safety Gate rejected: ${safetyResult.reason}"
            )

            return Setup(
                direction = SetupDirection.NONE,
                entryPrice = entryPrice,
                stopLoss = entryPrice,
                takeProfit = entryPrice,
                confidence = 0,
                reasons = reasons
            )
        }

        reasons.add(
            "Safety Gate approved"
        )

        return Setup(
            direction = direction,
            entryPrice = entryPrice,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            confidence = confidence,
            reasons = reasons
        )
    }

    private fun resolveDirection(
        analysis: MarketAnalysisResult,
        reasons: MutableList<String>
    ): SetupDirection {

        val trendUp =
            analysis.trend.direction == TrendDirection.UP

        val trendDown =
            analysis.trend.direction == TrendDirection.DOWN

        val structureBullish =
            analysis.structure.direction == StructureDirection.BULLISH

        val structureBearish =
            analysis.structure.direction == StructureDirection.BEARISH

        val locationDiscount =
            analysis.location.location == PriceLocation.DISCOUNT ||
            analysis.location.location == PriceLocation.NEAR_P25

        val locationPremium =
            analysis.location.location == PriceLocation.PREMIUM ||
            analysis.location.location == PriceLocation.NEAR_P75

        val buyAd =
            analysis.ad.type == ADType.BUY_AD

        val sellAd =
            analysis.ad.type == ADType.SELL_AD

        // BUY conditions
        if (
            trendUp &&
            structureBullish &&
            locationDiscount &&
            buyAd
        ) {
            reasons.add("Trend UP")
            reasons.add("Structure BULLISH")
            reasons.add("Location DISCOUNT")
            reasons.add("Buy AD detected")

            return SetupDirection.BUY
        }

        // SELL conditions
        if (
            trendDown &&
            structureBearish &&
            locationPremium &&
            sellAd
        ) {
            reasons.add("Trend DOWN")
            reasons.add("Structure BEARISH")
            reasons.add("Location PREMIUM")
            reasons.add("Sell AD detected")

            return SetupDirection.SELL
        }

        // No setup
        if (analysis.marketState == MarketState.UNCERTAIN) {
            reasons.add("Market state UNCERTAIN")
        } else {
            reasons.add("Conditions not fully met")
        }

        return SetupDirection.NONE
    }

    private fun resolveRange(
        entryPrice: Double,
        analysis: MarketAnalysisResult
    ): Double {

        // Simplified range:
        // 0.5% of entry price as the base range.
        val base = entryPrice * 0.005

        val strengthFactor =
            (100 - analysis.overallStrength) / 100.0

        return base * (1.0 + strengthFactor)
    }

    private fun resolveConfidence(
        analysis: MarketAnalysisResult
    ): Int {

        val weighted =
            analysis.trend.strength * 0.30 +
            analysis.structure.strength * 0.30 +
            analysis.location.strength * 0.20 +
            analysis.ad.strength * 0.20

        return weighted
            .toInt()
            .coerceIn(0, 100)
    }
}
