package engine.safety

data class SafetyCheckResult(
    val approved: Boolean,
    val reason: String,
    val score: Int
) {
    init {
        require(score in 0..100) {
            "score must be between 0 and 100"
        }
    }
}

object SafetyGate {

    fun check(
        hasValidSetup: Boolean,
        hasValidEntry: Boolean,
        hasValidStop: Boolean,
        hasValidTargets: Boolean,
        riskRewardRatio: Double,
        minimumRiskReward: Double = 1.5
    ): SafetyCheckResult {

        if (!hasValidSetup) {
            return SafetyCheckResult(
                approved = false,
                reason = "Invalid setup",
                score = 0
            )
        }

        if (!hasValidEntry) {
            return SafetyCheckResult(
                approved = false,
                reason = "Invalid entry",
                score = 20
            )
        }

        if (!hasValidStop) {
            return SafetyCheckResult(
                approved = false,
                reason = "Invalid stop loss",
                score = 30
            )
        }

        if (!hasValidTargets) {
            return SafetyCheckResult(
                approved = false,
                reason = "Invalid targets",
                score = 40
            )
        }

        if (!riskRewardRatio.isFinite() || riskRewardRatio <= 0.0) {
            return SafetyCheckResult(
                approved = false,
                reason = "Invalid risk reward ratio",
                score = 50
            )
        }

        if (riskRewardRatio < minimumRiskReward) {
            return SafetyCheckResult(
                approved = false,
                reason = "Risk reward ratio is below minimum",
                score = 60
            )
        }

        val score = when {
            riskRewardRatio >= 3.0 -> 100
            riskRewardRatio >= 2.5 -> 95
            riskRewardRatio >= 2.0 -> 90
            else -> 80
        }

        return SafetyCheckResult(
            approved = true,
            reason = "Setup passed safety checks",
            score = score
        )
    }
}
