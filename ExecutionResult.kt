package engine.core

enum class ExecutionOutcome {
    ALLOWED,
    BLOCKED,
    SIMULATED_PASS,
    SIMULATED_FAIL
}

data class ExecutionResult(
    val decisionId: String,
    val result: ExecutionOutcome,
    val executionConfidence: Int,
    val reasonCodes: List<ReasonCode>,
    val timestamp: Long
) {
    init {
        require(executionConfidence in 0..100) {
            "executionConfidence must be 0..100"
        }

        require(reasonCodes.none { it.domain == ReasonDomain.ANALYSIS }) {
            "ExecutionResult.reasonCodes must only contain DATA or EXECUTION codes"
        }

        if (result == ExecutionOutcome.BLOCKED) {
            require(reasonCodes.isNotEmpty()) {
                "A BLOCKED result must always carry at least one reason code"
            }
        }

        if (reasonCodes.any { it.isBlocking }) {
            require(
                result == ExecutionOutcome.BLOCKED ||
                result == ExecutionOutcome.SIMULATED_FAIL
            ) {
                "A CRITICAL reason code can never coexist with ALLOWED/SIMULATED_PASS"
            }

            require(executionConfidence == 0) {
                "A CRITICAL reason code must force executionConfidence to 0"
            }
        }
    }

    companion object {
        fun computeConfidence(
            checkScores: Map<String, Int>,
            reasonCodes: List<ReasonCode>
        ): Int {
            if (reasonCodes.any { it.isBlocking }) {
                return 0
            }

            return checkScores.values.minOrNull() ?: 0
        }
    }
}
