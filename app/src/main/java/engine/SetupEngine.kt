package engine.setup

import engine.analysis.ADType
import engine.analysis.MarketAnalysisResult
import engine.analysis.MarketState
import engine.analysis.PriceLocation
import engine.analysis.StructureDirection
import engine.analysis.TrendDirection
import engine.safety.SafetyGate
import kotlin.math.abs

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

    private const val MIN_SETUP_CONFIDENCE = 70

    /*
     * Base risk distance.
     *
     * This is deliberately kept conservative until the analysis layer
     * exposes ATR / swing-price information directly.
     *
     * Later this can be replaced by:
     * - ATR
     * - latest structural swing
     * - AD candle extreme
     * - liquidity sweep extreme
     * without changing the public Setup interface.
     */
    private const val BASE_RISK_PERCENT = 0.005

    private const val TAKE_PROFIT_R_MULTIPLE = 2.0

    fun build(
        analysis: MarketAnalysisResult,
        entryPrice: Double
    ): Setup {

        if (!entryPrice.isFinite() || entryPrice <= 0.0) {
            return noSetup(
                entryPrice = entryPrice,
                reasons = mutableListOf(
                    "Invalid entry price"
                )
            )
        }

        val reasons = mutableListOf<String>()

        val direction = resolveDirection(
            analysis = analysis,
            reasons = reasons
        )

        if (direction == SetupDirection.NONE) {
            return noSetup(
                entryPrice = entryPrice,
                reasons = reasons
            )
        }

        val confidence = resolveConfidence(
            analysis = analysis,
            direction = direction
        )

        reasons.add(
            "Setup confidence: $confidence"
        )

        if (confidence < MIN_SETUP_CONFIDENCE) {
            reasons.add(
                "Setup confidence below minimum threshold"
            )

            return noSetup(
                entryPrice = entryPrice,
                reasons = reasons
            )
        }

        val riskDistance = resolveRiskDistance(
            entryPrice = entryPrice,
            analysis = analysis
        )

        if (!riskDistance.isFinite() || riskDistance <= 0.0) {
            reasons.add("Invalid risk distance")

            return noSetup(
                entryPrice = entryPrice,
                reasons = reasons
            )
        }

        val stopLoss: Double
        val takeProfit: Double

        when (direction) {

            SetupDirection.BUY -> {
                stopLoss = entryPrice - riskDistance
                takeProfit =
                    entryPrice +
                        riskDistance * TAKE_PROFIT_R_MULTIPLE

                reasons.add("BUY risk distance calculated")
                reasons.add("TP set at 2R")
            }

            SetupDirection.SELL -> {
                stopLoss = entryPrice + riskDistance
                takeProfit =
                    entryPrice -
                        riskDistance * TAKE_PROFIT_R_MULTIPLE

                reasons.add("SELL risk distance calculated")
                reasons.add("TP set at 2R")
            }

            SetupDirection.NONE -> {
                return noSetup(
                    entryPrice = entryPrice,
                    reasons = reasons
                )
            }
        }

        val risk = abs(entryPrice - stopLoss)
        val reward = abs(takeProfit - entryPrice)

        val riskRewardRatio =
            if (risk > 0.0) {
                reward / risk
            } else {
                Double.NaN
            }

        reasons.add(
            "Risk/Reward = ${
                String.format("%.2f", riskRewardRatio)
            }"
        )

        val safetyResult = SafetyGate.check(
            hasValidSetup = true,
            hasValidEntry =
                entryPrice.isFinite() &&
                    entryPrice > 0.0,
            hasValidStop =
                stopLoss.isFinite() &&
                    stopLoss > 0.0,
            hasValidTargets =
                takeProfit.isFinite() &&
                    takeProfit > 0.0,
            riskRewardRatio = riskRewardRatio
        )

        if (!safetyResult.approved) {

            reasons.add(
                "Safety Gate rejected: ${safetyResult.reason}"
            )

            return noSetup(
                entryPrice = entryPrice,
                reasons = reasons
            )
        }

        reasons.add("Safety Gate approved")

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
            analysis.structure.direction ==
                StructureDirection.BULLISH

        val structureBearish =
            analysis.structure.direction ==
                StructureDirection.BEARISH

        val locationDiscount =
            analysis.location.location ==
                PriceLocation.DISCOUNT ||
                analysis.location.location ==
                PriceLocation.NEAR_P25

        val locationPremium =
            analysis.location.location ==
                PriceLocation.PREMIUM ||
                analysis.location.location ==
                PriceLocation.NEAR_P75

        val buyAd =
            analysis.ad.type == ADType.BUY_AD

        val sellAd =
            analysis.ad.type == ADType.SELL_AD

        /*
         * BUY
         *
         * Required confluence:
         * 1. Trend UP
         * 2. Bullish structure
         * 3. Price in discount / near P25
         * 4. BUY AD
         */
        if (
            trendUp &&
            structureBullish &&
            locationDiscount &&
            buyAd
        ) {
            reasons.add("Trend UP")
            reasons.add("Structure BULLISH")
            reasons.add("Price in DISCOUNT/P25")
            reasons.add("BUY AD detected")

            if (analysis.structure.hasBreakOfStructure) {
                reasons.add("Bullish BOS confirmed")
            }

            if (analysis.structure.hasChangeOfCharacter) {
                reasons.add("Change of Character detected")
            }

            return SetupDirection.BUY
        }

        /*
         * SELL
         *
         * Required confluence:
         * 1. Trend DOWN
         * 2. Bearish structure
         * 3. Price in premium / near P75
         * 4. SELL AD
         */
        if (
            trendDown &&
            structureBearish &&
            locationPremium &&
            sellAd
        ) {
            reasons.add("Trend DOWN")
            reasons.add("Structure BEARISH")
            reasons.add("Price in PREMIUM/P75")
            reasons.add("SELL AD detected")

            if (analysis.structure.hasBreakOfStructure) {
                reasons.add("Bearish BOS confirmed")
            }

            if (analysis.structure.hasChangeOfCharacter) {
                reasons.add("Change of Character detected")
            }

            return SetupDirection.SELL
        }

        if (analysis.marketState == MarketState.UNCERTAIN) {
            reasons.add("Market state UNCERTAIN")
        }

        if (analysis.trend.direction == TrendDirection.UNCERTAIN) {
            reasons.add("Trend uncertain")
        }

        if (
            analysis.structure.direction ==
                StructureDirection.UNCERTAIN
        ) {
            reasons.add("Structure uncertain")
        }

        if (analysis.ad.type == ADType.NONE) {
            reasons.add("No valid AD detected")
        }

        if (
            analysis.location.location ==
                PriceLocation.UNCERTAIN
        ) {
            reasons.add("Price location uncertain")
        }

        reasons.add("Required confluence not complete")

        return SetupDirection.NONE
    }

    private fun resolveRiskDistance(
        entryPrice: Double,
        analysis: MarketAnalysisResult
    ): Double {

        /*
         * Until ATR / swing prices are exposed by MarketAnalysisResult,
         * use a deterministic base distance.
         *
         * The distance is adjusted slightly by analysis strength:
         * stronger analysis -> tighter base risk
         * weaker analysis -> wider base risk
         *
         * This keeps the interface stable while avoiding dependence
         * on random/mock candle values.
         */
        val baseDistance =
            entryPrice * BASE_RISK_PERCENT

        val strengthFactor =
            when {
                analysis.overallStrength >= 80 -> 0.85
                analysis.overallStrength >= 70 -> 0.95
                analysis.overallStrength >= 60 -> 1.00
                else -> 1.10
            }

        return baseDistance * strengthFactor
    }

    private fun resolveConfidence(
        analysis: MarketAnalysisResult,
        direction: SetupDirection
    ): Int {

        var score =
            analysis.trend.strength * 0.30 +
            analysis.structure.strength * 0.30 +
            analysis.location.strength * 0.20 +
            analysis.ad.strength * 0.20

        /*
         * Additional structural confirmation.
         *
         * BOS strengthens the setup.
         * CHoCH is useful confirmation but should not dominate
         * the score by itself.
         */
        if (analysis.structure.hasBreakOfStructure) {
            score += 5.0
        }

        if (analysis.structure.hasChangeOfCharacter) {
            score += 3.0
        }

        /*
         * Direction consistency check.
         */
        val directionConsistent =
            when (direction) {
                SetupDirection.BUY ->
                    analysis.trend.direction ==
                        TrendDirection.UP &&
                        analysis.structure.direction ==
                        StructureDirection.BULLISH

                SetupDirection.SELL ->
                    analysis.trend.direction ==
                        TrendDirection.DOWN &&
                        analysis.structure.direction ==
                        StructureDirection.BEARISH

                SetupDirection.NONE -> false
            }

        if (!directionConsistent) {
            score -= 10.0
        }

        return score
            .toInt()
            .coerceIn(0, 100)
    }

    private fun noSetup(
        entryPrice: Double,
        reasons: MutableList<String>
    ): Setup {

        return Setup(
            direction = SetupDirection.NONE,
            entryPrice = entryPrice,
            stopLoss = entryPrice,
            takeProfit = entryPrice,
            confidence = 0,
            reasons = reasons
        )
    }
}
