package engine.decision

import engine.analysis.MarketAnalysisResult
import engine.setup.Setup
import engine.setup.SetupDirection

enum class DecisionType {
    BUY,
    SELL,
    WAIT
}

data class DecisionResult(
    val type: DecisionType,
    val confidence: Int,
    val reasons: List<String>
) {
    init {
        require(confidence in 0..100) {
            "confidence must be between 0 and 100"
        }
    }
}

object DecisionEngine {

    fun build(
        analysis: MarketAnalysisResult,
        setup: Setup
    ): DecisionResult {

        val reasons = mutableListOf<String>()

        if (setup.direction == SetupDirection.NONE) {
            reasons.add("No valid setup")

            if (analysis.marketState.name == "UNCERTAIN") {
                reasons.add("Market state is uncertain")
            }

            return DecisionResult(
                type = DecisionType.WAIT,
                confidence = 0,
                reasons = reasons
            )
        }

        val type = when (setup.direction) {
            SetupDirection.BUY -> DecisionType.BUY
            SetupDirection.SELL -> DecisionType.SELL
            SetupDirection.NONE -> DecisionType.WAIT
        }

        reasons.addAll(setup.reasons)

        reasons.add(
            "Setup confidence: ${setup.confidence}"
        )

        val finalConfidence = (
            setup.confidence * 0.70 +
            analysis.overallStrength * 0.30
        ).toInt().coerceIn(0, 100)

        return DecisionResult(
            type = type,
            confidence = finalConfidence,
            reasons = reasons
        )
    }
}
