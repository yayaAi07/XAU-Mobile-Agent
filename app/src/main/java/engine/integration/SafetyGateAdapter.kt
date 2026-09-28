package engine.integration

import engine.core.Decision
import engine.core.ExecutionOutcome
import engine.core.ExecutionResult
import engine.core.ReasonCode
import engine.core.SafetyGate as CoreSafetyGate
import engine.safety.SafetyGate as SetupSafetyGate
import engine.setup.Setup
import engine.setup.SetupDirection
import kotlin.math.abs

class SafetyGateAdapter(
    private val setupContextStore: SetupContextStore,
    private val minimumRiskReward: Double = 1.5
) : CoreSafetyGate {

    override fun verify(
        decision: Decision,
        now: Long
    ): ExecutionResult {

        if (decision.isExpiredAt(now)) {
            setupContextStore.remove(decision.decisionId)

            return blocked(
                decision,
                now,
                ReasonCode.DECISION_EXPIRED
            )
        }

        val setup =
            setupContextStore.get(decision.decisionId)

        if (setup == null) {
            return blocked(
                decision,
                now,
                ReasonCode.SETUP_CONTEXT_MISSING
            )
        }

        setupContextStore.remove(decision.decisionId)

        val hasValidEntry =
            isValidEntry(setup)

        val hasValidStop =
            isValidStop(setup)

        val hasValidTargets =
            isValidTarget(setup)

        val hasValidSetup =
            setup.direction != SetupDirection.NONE &&
                hasValidEntry &&
                hasValidStop &&
                hasValidTargets

        val riskRewardRatio =
            computeRiskReward(
                setup.entryPrice,
                setup.stopLoss,
                setup.takeProfit
            )

        val safetyResult =
            SetupSafetyGate.check(
                hasValidSetup = hasValidSetup,
                hasValidEntry = hasValidEntry,
                hasValidStop = hasValidStop,
                hasValidTargets = hasValidTargets,
                riskRewardRatio = riskRewardRatio,
                minimumRiskReward = minimumRiskReward
            )

        if (safetyResult.approved) {
            return ExecutionResult(
                decisionId = decision.decisionId,
                result = ExecutionOutcome.ALLOWED,
                executionConfidence = safetyResult.score,
                reasonCodes = emptyList(),
                timestamp = now
            )
        }

        val reasonCode =
            when {
                !hasValidSetup ->
                    ReasonCode.SETUP_INVALID

                !hasValidEntry ->
                    ReasonCode.ENTRY_INVALID

                !hasValidStop ->
                    ReasonCode.STOP_INVALID

                !hasValidTargets ->
                    ReasonCode.TARGETS_INVALID

                !riskRewardRatio.isFinite() ||
                    riskRewardRatio <= 0.0 ->
                    ReasonCode.RISK_REWARD_BELOW_MINIMUM

                riskRewardRatio < minimumRiskReward ->
                    ReasonCode.RISK_REWARD_BELOW_MINIMUM

                else ->
                    ReasonCode.SETUP_INVALID
            }

        return blocked(
            decision,
            now,
            reasonCode
        )
    }

    private fun isValidEntry(
        setup: Setup
    ): Boolean =
        setup.entryPrice.isFinite() &&
            setup.entryPrice > 0.0

    private fun isValidStop(
        setup: Setup
    ): Boolean {

        if (
            !setup.stopLoss.isFinite() ||
            setup.stopLoss <= 0.0
        ) {
            return false
        }

        return when (setup.direction) {
            SetupDirection.BUY ->
                setup.stopLoss < setup.entryPrice

            SetupDirection.SELL ->
                setup.stopLoss > setup.entryPrice

            SetupDirection.NONE ->
                false
        }
    }

    private fun isValidTarget(
        setup: Setup
    ): Boolean {

        if (
            !setup.takeProfit.isFinite() ||
            setup.takeProfit <= 0.0
        ) {
            return false
        }

        return when (setup.direction) {
            SetupDirection.BUY ->
                setup.takeProfit > setup.entryPrice

            SetupDirection.SELL ->
                setup.takeProfit < setup.entryPrice

            SetupDirection.NONE ->
                false
        }
    }

    private fun computeRiskReward(
        entry: Double,
        stop: Double,
        target: Double
    ): Double {

        val risk = abs(entry - stop)
        val reward = abs(target - entry)

        if (
            !risk.isFinite() ||
            !reward.isFinite() ||
            risk <= 0.0
        ) {
            return Double.NaN
        }

        return reward / risk
    }

    private fun blocked(
        decision: Decision,
        now: Long,
        reasonCode: ReasonCode
    ): ExecutionResult =
        ExecutionResult(
            decisionId = decision.decisionId,
            result = ExecutionOutcome.BLOCKED,
            executionConfidence = 0,
            reasonCodes = listOf(reasonCode),
            timestamp = now
        )
}
