package engine.core

interface Clock {
    fun now(): Long
}

class SystemClock : Clock {
    override fun now(): Long = System.currentTimeMillis()
}

interface RetryScheduler {
    fun waitUntil(timestampMillis: Long, clock: Clock)
}

/**
 * Reference/placeholder implementation only.
 *
 * Thread.sleep() must NOT run on Android's UI thread.
 * Before integrating this into the Android app, replace it
 * with a coroutine delay() or Handler-based implementation.
 */
class BlockingRetryScheduler : RetryScheduler {

    override fun waitUntil(
        timestampMillis: Long,
        clock: Clock
    ) {
        val delay = timestampMillis - clock.now()

        if (delay > 0) {
            Thread.sleep(delay)
        }
    }
}

data class DataValidationResult(
    val reasonCodes: List<ReasonCode>
) {
    init {
        require(
            reasonCodes.all {
                it.domain == ReasonDomain.DATA
            }
        ) {
            "DataValidationResult.reasonCodes must only contain DATA codes"
        }
    }

    val isFresh: Boolean
        get() =
            reasonCodes.none {
                it.isBlocking || it.allowsRetry
            }
}

interface DataValidator {
    fun validate(now: Long): DataValidationResult
}

interface AnalysisEngine {
    fun analyze(
        now: Long,
        parentDecisionId: String? = null
    ): Decision?
}

interface SafetyGate {
    fun verify(
        decision: Decision,
        now: Long
    ): ExecutionResult
}

interface VirtualExecutionEngine {
    fun simulate(
        decision: Decision,
        now: Long
    ): ExecutionResult
}

interface HealthChecker {
    fun check(now: Long): HealthCheckResult
}

interface StateMachineLogger {
    fun log(
        from: SystemState,
        to: SystemState,
        detail: String
    )
}

class PrintlnStateMachineLogger : StateMachineLogger {

    override fun log(
        from: SystemState,
        to: SystemState,
        detail: String
    ) {
        println(
            "[StateMachine] $from -> $to :: $detail"
        )
    }
}

class StateMachine(
    private val clock: Clock = SystemClock(),
    private val scheduler: RetryScheduler = BlockingRetryScheduler(),
    private val dataValidator: DataValidator,
    private val analysisEngine: AnalysisEngine,
    private val safetyGate: SafetyGate,
    private val virtualExecutionEngine: VirtualExecutionEngine,
    private val healthChecker: HealthChecker,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val logger: StateMachineLogger =
        PrintlnStateMachineLogger(),
    private val maxReanalysisChain: Int = 3
) {

    init {
        require(maxReanalysisChain > 0) {
            "maxReanalysisChain must be greater than 0"
        }
    }

    var state: SystemState = SystemState.WAIT
        private set

    private fun transition(
        to: SystemState,
        detail: String
    ) {
        logger.log(
            state,
            to,
            detail
        )

        state = to
    }

    fun runCycle(): SystemState {

        transition(
            SystemState.SCANNING,
            "starting scan"
        )

        val dataResult =
            dataValidator.validate(clock.now())

        if (!dataResult.isFresh) {

            val nextState =
                StateTransitionPolicy.nextStateAfterBlock(
                    reasonCodes = dataResult.reasonCodes,
                    retryBudgetExhausted = false
                )

            transition(
                nextState,
                "data validation failed: ${dataResult.reasonCodes}"
            )

            return nextState
        }

        transition(
            SystemState.ANALYZING,
            "running analysis engine"
        )

        val decision =
            analysisEngine.analyze(clock.now())

        if (decision == null) {

            transition(
                SystemState.WAIT,
                "no setup found"
            )

            return SystemState.WAIT
        }

        transition(
            SystemState.DECISION_CREATED,
            "decision=${decision.decisionId} " +
                "direction=${decision.direction} " +
                "confidence=${decision.analysisConfidence} " +
                "parent=${decision.parentDecisionId}"
        )

        val retryState =
            retryPolicy.initialState(
                decisionId = decision.decisionId,
                decision = decision,
                now = clock.now()
            )

        return runPreExecutionLoop(
            decision,
            retryState,
            reanalysisDepth = 0
        )
    }

    private fun runPreExecutionLoop(
        initialDecision: Decision,
        initialRetryState: RetryState,
        reanalysisDepth: Int
    ): SystemState {

        var decision = initialDecision
        var retryState = initialRetryState
        var depth = reanalysisDepth

        while (true) {

            transition(
                SystemState.PRE_EXECUTION_CHECK,
                "decision=${decision.decisionId} " +
                    "attempt=${retryState.attemptsUsed}"
            )

            val gateResult =
                safetyGate.verify(
                    decision,
                    clock.now()
                )

            when (gateResult.result) {

                ExecutionOutcome.ALLOWED -> {
                    return runVirtualExecution(
                        decision,
                        retryState,
                        depth
                    )
                }

                ExecutionOutcome.BLOCKED -> {

                    val retryDecision =
                        retryPolicy.evaluate(
                            gateResult.reasonCodes,
                            decision,
                            retryState,
                            clock.now()
                        )

                    val outcome =
                        handleBlocked(
                            decision = decision,
                            retryState = retryState,
                            reasonCodes = gateResult.reasonCodes,
                            retryDecision = retryDecision,
                            depth = depth
                        )

                    when (outcome) {

                        is CycleOutcome.Continue -> {
                            decision = outcome.decision
                            retryState = outcome.retryState
                            depth = outcome.depth
                        }

                        is CycleOutcome.Terminal ->
                            return outcome.state
                    }
                }

                ExecutionOutcome.SIMULATED_PASS,
                ExecutionOutcome.SIMULATED_FAIL -> {

                    transition(
                        SystemState.BLOCKED,
                        "safety gate returned an unexpected simulated outcome"
                    )

                    return SystemState.BLOCKED
                }
            }
        }
    }

    private fun runVirtualExecution(
        decision: Decision,
        retryState: RetryState,
        depth: Int
    ): SystemState {

        transition(
            SystemState.VIRTUAL_EXECUTION,
            "simulating execution for decision=${decision.decisionId}"
        )

        val simResult =
            virtualExecutionEngine.simulate(
                decision,
                clock.now()
            )

        return when (simResult.result) {

            ExecutionOutcome.SIMULATED_PASS -> {

                transition(
                    SystemState.VERIFY,
                    "virtual execution passed, " +
                        "confidence=${simResult.executionConfidence}"
                )

                transition(
                    SystemState.MONITORING,
                    "monitoring simulated position"
                )

                transition(
                    SystemState.EXIT,
                    "simulated position closed"
                )

                transition(
                    SystemState.COOLDOWN,
                    "cooldown before next scan"
                )

                transition(
                    SystemState.WAIT,
                    "cycle complete"
                )

                SystemState.WAIT
            }

            ExecutionOutcome.SIMULATED_FAIL -> {

                val retryDecision =
                    retryPolicy.evaluate(
                        simResult.reasonCodes,
                        decision,
                        retryState,
                        clock.now()
                    )

                val outcome =
                    handleBlocked(
                        decision = decision,
                        retryState = retryState,
                        reasonCodes = simResult.reasonCodes,
                        retryDecision = retryDecision,
                        depth = depth
                    )

                when (outcome) {

                    is CycleOutcome.Continue ->
                        runPreExecutionLoop(
                            outcome.decision,
                            outcome.retryState,
                            outcome.depth
                        )

                    is CycleOutcome.Terminal ->
                        outcome.state
                }
            }

            else -> {

                transition(
                    SystemState.BLOCKED,
                    "virtual execution returned an unexpected outcome"
                )

                SystemState.BLOCKED
            }
        }
    }

    private sealed class CycleOutcome {

        data class Continue(
            val decision: Decision,
            val retryState: RetryState,
            val depth: Int
        ) : CycleOutcome()

        data class Terminal(
            val state: SystemState
        ) : CycleOutcome()
    }

    private fun handleBlocked(
        decision: Decision,
        retryState: RetryState,
        reasonCodes: List<ReasonCode>,
        retryDecision: RetryDecision,
        depth: Int
    ): CycleOutcome {

        val budgetExhausted =
            retryDecision is RetryDecision.Exhausted

        val nextState =
            StateTransitionPolicy.nextStateAfterBlock(
                reasonCodes = reasonCodes,
                retryBudgetExhausted = budgetExhausted
            )

        transition(
            nextState,
            "decision=${decision.decisionId} " +
                "reasonCodes=$reasonCodes"
        )

        return when (nextState) {

            SystemState.BLOCKED ->
                CycleOutcome.Terminal(
                    SystemState.BLOCKED
                )

            SystemState.RE_ANALYZE -> {

                if (depth >= maxReanalysisChain) {

                    transition(
                        SystemState.DEGRADED,
                        "re-analysis chain exceeded " +
                            "$maxReanalysisChain attempts"
                    )

                    return CycleOutcome.Terminal(
                        runDegradedRecovery()
                    )
                }

                val newDecision =
                    analysisEngine.analyze(
                        clock.now(),
                        parentDecisionId = decision.decisionId
                    )

                if (newDecision == null) {

                    transition(
                        SystemState.WAIT,
                        "re-analysis found no further setup"
                    )

                    CycleOutcome.Terminal(
                        SystemState.WAIT
                    )

                } else {

                    val newRetryState =
                        retryPolicy.initialState(
                            decisionId = newDecision.decisionId,
                            decision = newDecision,
                            now = clock.now()
                        )

                    CycleOutcome.Continue(
                        newDecision,
                        newRetryState,
                        depth + 1
                    )
                }
            }

            SystemState.DATA_FAILURE ->
                CycleOutcome.Terminal(
                    SystemState.DATA_FAILURE
                )

            SystemState.RESCAN -> {

                when (val rd = retryDecision) {

                    is RetryDecision.Retry -> {

                        scheduler.waitUntil(
                            rd.attempt.scheduledAt,
                            clock
                        )

                        CycleOutcome.Continue(
                            decision,
                            retryState.withAttemptRecorded(
                                clock.now()
                            ),
                            depth
                        )
                    }

                    RetryDecision.Exhausted -> {

                        transition(
                            SystemState.DEGRADED,
                            "retry budget exhausted for " +
                                "decision=${decision.decisionId}"
                        )

                        CycleOutcome.Terminal(
                            runDegradedRecovery()
                        )
                    }

                    RetryDecision.NotRetryable ->
                        CycleOutcome.Terminal(
                            SystemState.BLOCKED
                        )
                }
            }

            SystemState.DEGRADED ->
                CycleOutcome.Terminal(
                    runDegradedRecovery()
                )

            else ->
                CycleOutcome.Terminal(
                    nextState
                )
        }
    }

    private fun runDegradedRecovery(): SystemState {

        transition(
            SystemState.HEALTH_CHECK,
            "probing health after DEGRADED"
        )

        val health =
            healthChecker.check(
                clock.now()
            )

        val next =
            StateTransitionPolicy.nextStateAfterHealthCheck(
                health.allChecksPassed
            )

        transition(
            next,
            "healthCheck=$health"
        )

        return if (next == SystemState.RECOVERED) {

            val backToWait =
                StateTransitionPolicy.nextStateAfterRecovered()

            transition(
                backToWait,
                "recovered, resuming normal operation"
            )

            backToWait

        } else {

            SystemState.DEGRADED
        }
    }
}
