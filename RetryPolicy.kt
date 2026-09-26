package engine.core

data class RetryAttempt(
    val attemptNumber: Int,
    val scheduledAt: Long,
    val backoffMillis: Long
)

sealed class RetryDecision {
    data class Retry(val attempt: RetryAttempt) : RetryDecision()
    object Exhausted : RetryDecision()
    object NotRetryable : RetryDecision()
}

data class RetryState(
    val decisionId: String,
    val schedule: List<RetryAttempt> = emptyList(),
    val attemptsUsed: Int = 0,
    val lastAttemptAt: Long? = null
) {
    fun withAttemptRecorded(now: Long): RetryState =
        copy(
            attemptsUsed = attemptsUsed + 1,
            lastAttemptAt = now
        )
}

class RetryPolicy(
    private val maxRetries: Int = 3,
    private val budgetFraction: Double = 0.6
) {

    init {
        require(maxRetries > 0) {
            "maxRetries must be greater than 0"
        }

        require(budgetFraction in 0.0..1.0) {
            "budgetFraction must be between 0.0 and 1.0"
        }
    }

    fun buildSchedule(
        decision: Decision,
        now: Long
    ): List<RetryAttempt> {

        val remaining = decision.remainingLifetimeMillis(now)

        val budget = (remaining * budgetFraction).toLong()

        if (budget <= 0L || maxRetries <= 0) {
            return emptyList()
        }

        val weights = (0 until maxRetries).map { index ->
            1L shl index
        }

        val totalWeight = weights.sum()

        if (totalWeight <= 0L) {
            return emptyList()
        }

        var elapsed = 0L

        val candidates = weights.mapIndexed { index, weight ->

            val slot = (budget * weight) / totalWeight

            elapsed += slot

            RetryAttempt(
                attemptNumber = index + 1,
                scheduledAt = now + elapsed,
                backoffMillis = slot
            )
        }

        return candidates.takeWhile {
            it.scheduledAt < decision.expiresAt
        }
    }

    fun initialState(
        decisionId: String,
        decision: Decision,
        now: Long
    ): RetryState =
        RetryState(
            decisionId = decisionId,
            schedule = buildSchedule(decision, now)
        )

    fun isBudgetExhausted(
        decision: Decision,
        attemptsUsed: Int,
        now: Long
    ): Boolean =
        attemptsUsed >= maxRetries ||
        decision.isExpiredAt(now)

    fun shouldRetry(
        reasonCodes: List<ReasonCode>,
        decision: Decision,
        attemptsUsed: Int,
        now: Long
    ): Boolean {

        if (reasonCodes.any { it.isBlocking || it.requiresReanalysis }) {
            return false
        }

        if (!reasonCodes.any { it.allowsRetry }) {
            return false
        }

        return !isBudgetExhausted(
            decision = decision,
            attemptsUsed = attemptsUsed,
            now = now
        )
    }

    fun evaluate(
        reasonCodes: List<ReasonCode>,
        decision: Decision,
        state: RetryState,
        now: Long
    ): RetryDecision {

        if (
            reasonCodes.any {
                it.isBlocking || it.requiresReanalysis
            } ||
            !reasonCodes.any { it.allowsRetry }
        ) {
            return RetryDecision.NotRetryable
        }

        if (
            isBudgetExhausted(
                decision = decision,
                attemptsUsed = state.attemptsUsed,
                now = now
            )
        ) {
            return RetryDecision.Exhausted
        }

        val schedule =
            state.schedule.ifEmpty {
                buildSchedule(decision, now)
            }

        val nextAttempt =
            schedule.getOrNull(state.attemptsUsed)
                ?: return RetryDecision.Exhausted

        return RetryDecision.Retry(nextAttempt)
    }
}
