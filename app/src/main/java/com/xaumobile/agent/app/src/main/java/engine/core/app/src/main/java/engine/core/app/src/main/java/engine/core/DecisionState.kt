package engine.core

enum class SystemState {
    WAIT,
    SCANNING,
    ANALYZING,
    DECISION_CREATED,
    PRE_EXECUTION_CHECK,
    VIRTUAL_EXECUTION,
    VERIFY,
    MONITORING,
    EXIT,
    COOLDOWN,

    BLOCKED,
    DATA_FAILURE,
    RESCAN,
    RE_ANALYZE,
    DEGRADED,
    HEALTH_CHECK,
    RECOVERED
}

object StateTransitionPolicy {

    fun nextStateAfterBlock(
        reasonCodes: List<ReasonCode>,
        retryBudgetExhausted: Boolean
    ): SystemState = when {
        reasonCodes.any { it.isBlocking } ->
            SystemState.BLOCKED

        reasonCodes.any { it.requiresReanalysis } ->
            SystemState.RE_ANALYZE

        retryBudgetExhausted ->
            SystemState.DEGRADED

        reasonCodes.any { it.domain == ReasonDomain.DATA } ->
            SystemState.DATA_FAILURE

        reasonCodes.any { it.allowsRetry } ->
            SystemState.RESCAN

        else ->
            SystemState.BLOCKED
    }

    fun nextStateAfterHealthCheck(
        allChecksPassed: Boolean
    ): SystemState =
        if (allChecksPassed) {
            SystemState.RECOVERED
        } else {
            SystemState.DEGRADED
        }

    fun nextStateAfterRecovered(): SystemState =
        SystemState.WAIT
}

data class HealthCheckResult(
    val ocrOk: Boolean,
    val accessibilityOk: Boolean,
    val screenRecognitionOk: Boolean,
    val dataFreshnessOk: Boolean
) {
    val allChecksPassed: Boolean
        get() =
            ocrOk &&
            accessibilityOk &&
            screenRecognitionOk &&
            dataFreshnessOk
}
