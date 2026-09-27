package engine.core

import org.junit.Assert.assertEquals
import org.junit.Test

class StateMachineIntegrationTest {

    private class FixedClock(
        private var currentTime: Long
    ) : Clock {

        override fun now(): Long = currentTime

        fun advance(millis: Long) {
            currentTime += millis
        }
    }

    private class NoWaitRetryScheduler : RetryScheduler {
        override fun waitUntil(
            timestampMillis: Long,
            clock: Clock
        ) {
            // Test scheduler: no real waiting.
        }
    }

    private class TestDataValidator : DataValidator {
        override fun validate(
            now: Long
        ): DataValidationResult {
            return DataValidationResult(
                reasonCodes = emptyList()
            )
        }
    }

    private class TestAnalysisEngine : AnalysisEngine {

        override fun analyze(
            now: Long,
            parentDecisionId: String?
        ): Decision {

            return Decision(
                decisionId = "test-decision",
                parentDecisionId = parentDecisionId,
                symbol = "XAUUSD",
                direction = Direction.BUY,
                createdAt = now,
                expiresAt = now + 60_000L,
                sourceTimeframe = SourceTimeframe.M15,
                analysisConfidence = 80,
                marketState = "TREND_UP",
                reasonCodes = listOf(
                    ReasonCode.TREND_UP,
                    ReasonCode.BUY_AD_DETECTED
                )
            )
        }
    }

    private class TestSafetyGate : SafetyGate {

        override fun verify(
            decision: Decision,
            now: Long
        ): ExecutionResult {

            return ExecutionResult(
                result = ExecutionOutcome.ALLOWED,
                reasonCodes = emptyList(),
                executionConfidence = 100
            )
        }
    }

    private class TestVirtualExecutionEngine :
        VirtualExecutionEngine {

        override fun simulate(
            decision: Decision,
            now: Long
        ): ExecutionResult {

            return ExecutionResult(
                result = ExecutionOutcome.SIMULATED_PASS,
                reasonCodes = emptyList(),
                executionConfidence = 100
            )
        }
    }

    private class TestHealthChecker : HealthChecker {

        override fun check(
            now: Long
        ): HealthCheckResult {

            return HealthCheckResult(
                allChecksPassed = true
            )
        }
    }

    private class TestLogger : StateMachineLogger {

        override fun log(
            from: SystemState,
            to: SystemState,
            detail: String
        ) {
            // Silence test logs.
        }
    }

    @Test
    fun state_machine_can_complete_full_cycle() {

        val clock =
            FixedClock(
                currentTime = 1_000_000L
            )

        val stateMachine =
            StateMachine(
                clock = clock,
                scheduler = NoWaitRetryScheduler(),
                dataValidator = TestDataValidator(),
                analysisEngine = TestAnalysisEngine(),
                safetyGate = TestSafetyGate(),
                virtualExecutionEngine =
                    TestVirtualExecutionEngine(),
                healthChecker = TestHealthChecker(),
                logger = TestLogger()
            )

        val finalState =
            stateMachine.runCycle()

        assertEquals(
            SystemState.WAIT,
            finalState
        )

        assertEquals(
            SystemState.WAIT,
            stateMachine.state
        )
    }
}
