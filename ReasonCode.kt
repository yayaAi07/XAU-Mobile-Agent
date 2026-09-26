package engine.core

enum class ReasonPolicy {
    CRITICAL,
    RETRYABLE,
    REANALYZE,
    NON_CRITICAL
}

enum class ReasonCode(
    val domain: ReasonDomain,
    val policy: ReasonPolicy
) {

    // ---- ANALYSIS_* : supporting evidence for a Decision ----
    TREND_UP(ReasonDomain.ANALYSIS, ReasonPolicy.NON_CRITICAL),
    TREND_DOWN(ReasonDomain.ANALYSIS, ReasonPolicy.NON_CRITICAL),
    TREND_WEAKENING(ReasonDomain.ANALYSIS, ReasonPolicy.NON_CRITICAL),
    STRUCTURE_WEAKENING(ReasonDomain.ANALYSIS, ReasonPolicy.NON_CRITICAL),
    STRUCTURE_SHIFT(ReasonDomain.ANALYSIS, ReasonPolicy.NON_CRITICAL),

    PRICE_NEAR_P25(ReasonDomain.ANALYSIS, ReasonPolicy.NON_CRITICAL),
    PRICE_NEAR_P75(ReasonDomain.ANALYSIS, ReasonPolicy.NON_CRITICAL),

    BUY_AD_DETECTED(ReasonDomain.ANALYSIS, ReasonPolicy.NON_CRITICAL),
    SELL_AD_DETECTED(ReasonDomain.ANALYSIS, ReasonPolicy.NON_CRITICAL),
    AD_BREAK_CONFIRMED(ReasonDomain.ANALYSIS, ReasonPolicy.NON_CRITICAL),

    CONFIRMATION_MISSING(ReasonDomain.ANALYSIS, ReasonPolicy.NON_CRITICAL),

    // ---- DATA_* : problems with input/data ----
    DATA_OCR_LOW_CONFIDENCE(
        ReasonDomain.DATA,
        ReasonPolicy.RETRYABLE
    ),

    DATA_PRICE_UNREADABLE(
        ReasonDomain.DATA,
        ReasonPolicy.RETRYABLE
    ),

    DATA_SCREEN_NOT_RECOGNIZED(
        ReasonDomain.DATA,
        ReasonPolicy.RETRYABLE
    ),

    DATA_CHART_NOT_DETECTED(
        ReasonDomain.DATA,
        ReasonPolicy.RETRYABLE
    ),

    DATA_SYMBOL_UNCERTAIN(
        ReasonDomain.DATA,
        ReasonPolicy.RETRYABLE
    ),

    DATA_TIMEFRAME_UNCERTAIN(
        ReasonDomain.DATA,
        ReasonPolicy.RETRYABLE
    ),

    DATA_STALE(
        ReasonDomain.DATA,
        ReasonPolicy.RETRYABLE
    ),

    DATA_RESOLUTION_MISMATCH(
        ReasonDomain.DATA,
        ReasonPolicy.CRITICAL
    ),

    DATA_ACCESSIBILITY_PERMISSION_LOST(
        ReasonDomain.DATA,
        ReasonPolicy.CRITICAL
    ),

    // ---- EXECUTION_* : Safety Gate outcomes ----
    SYMBOL_MISMATCH(
        ReasonDomain.EXECUTION,
        ReasonPolicy.CRITICAL
    ),

    TIMEFRAME_MISMATCH(
        ReasonDomain.EXECUTION,
        ReasonPolicy.CRITICAL
    ),

    PRICE_SANITY_FAILED(
        ReasonDomain.EXECUTION,
        ReasonPolicy.CRITICAL
    ),

    CHART_NOT_RECOGNIZED(
        ReasonDomain.EXECUTION,
        ReasonPolicy.RETRYABLE
    ),

    PRICE_NOT_READABLE(
        ReasonDomain.EXECUTION,
        ReasonPolicy.RETRYABLE
    ),

    CONFLICTING_STATE(
        ReasonDomain.EXECUTION,
        ReasonPolicy.RETRYABLE
    ),

    TARGET_NOT_FOUND(
        ReasonDomain.EXECUTION,
        ReasonPolicy.RETRYABLE
    ),

    INTERFACE_UNSTABLE(
        ReasonDomain.EXECUTION,
        ReasonPolicy.RETRYABLE
    ),

    VIRTUAL_CLICK_FAILED(
        ReasonDomain.EXECUTION,
        ReasonPolicy.RETRYABLE
    ),

    DECISION_EXPIRED(
        ReasonDomain.EXECUTION,
        ReasonPolicy.REANALYZE
    ),

    // ---- Positive verification outcomes ----
    SYMBOL_VERIFIED(
        ReasonDomain.EXECUTION,
        ReasonPolicy.NON_CRITICAL
    ),

    TIMEFRAME_VERIFIED(
        ReasonDomain.EXECUTION,
        ReasonPolicy.NON_CRITICAL
    ),

    PRICE_VERIFIED(
        ReasonDomain.EXECUTION,
        ReasonPolicy.NON_CRITICAL
    ),

    TARGET_VERIFIED(
        ReasonDomain.EXECUTION,
        ReasonPolicy.NON_CRITICAL
    ),

    INTERFACE_VERIFIED(
        ReasonDomain.EXECUTION,
        ReasonPolicy.NON_CRITICAL
    );

    val isBlocking: Boolean
        get() = policy == ReasonPolicy.CRITICAL

    val allowsRetry: Boolean
        get() = policy == ReasonPolicy.RETRYABLE

    val requiresReanalysis: Boolean
        get() = policy == ReasonPolicy.REANALYZE
}
