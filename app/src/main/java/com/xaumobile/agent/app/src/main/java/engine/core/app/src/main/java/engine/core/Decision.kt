package engine.core

enum class Direction {
    BUY,
    SELL,
    WAIT
}

enum class SourceTimeframe(val approxLifetimeMillis: Long) {
    M1(4_000),
    M5(12_000),
    M15(45_000),
    H1(120_000),
    H4(300_000)
}

data class Decision(
    val decisionId: String,
    val parentDecisionId: String? = null,
    val symbol: String,
    val direction: Direction,
    val createdAt: Long,
    val expiresAt: Long,
    val sourceTimeframe: SourceTimeframe,
    val analysisConfidence: Int,
    val marketState: String,
    val reasonCodes: List<ReasonCode>
) {
    init {
        require(analysisConfidence in 0..100) {
            "analysisConfidence must be 0..100"
        }

        require(expiresAt > createdAt) {
            "expiresAt must be after createdAt"
        }

        require(reasonCodes.all { it.domain == ReasonDomain.ANALYSIS }) {
            "Decision.reasonCodes must only contain ANALYSIS codes"
        }
    }

    fun isExpiredAt(nowMillis: Long): Boolean =
        nowMillis >= expiresAt

    fun remainingLifetimeMillis(nowMillis: Long): Long =
        (expiresAt - nowMillis).coerceAtLeast(0)
}
