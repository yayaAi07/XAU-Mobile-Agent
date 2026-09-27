package engine.decision

import engine.analysis.ADType
import engine.analysis.PriceLocation
import engine.analysis.TrendDirection
import engine.core.AnalysisEngine
import engine.core.Decision
import engine.core.Direction
import engine.core.ReasonCode
import engine.core.SourceTimeframe
import engine.data.MarketDataRepository
import engine.data.Timeframe
import engine.setup.SetupDirection
import java.util.UUID

class DecisionAnalysisAdapter(
    private val repository: MarketDataRepository,
    private val symbol: String = "XAUUSD"
) : AnalysisEngine {

    override fun analyze(
        now: Long,
        parentDecisionId: String?
    ): Decision? {

        val snapshot = repository.getSnapshot(
            symbol = symbol,
            now = now
        )

        val entryPrice =
            snapshot.latest(Timeframe.M15)?.close
                ?: return null

        if (!entryPrice.isFinite() || entryPrice <= 0.0) {
            return null
        }

        val analysis =
            MarketAnalysisEngine.analyze(snapshot)

        val setup =
            SetupEngine.build(
                analysis = analysis,
                entryPrice = entryPrice
            )

        val decisionResult =
            DecisionEngine.build(
                analysis = analysis,
                setup = setup
            )

        if (decisionResult.type == DecisionType.WAIT) {
            return null
        }

        val direction =
            when (decisionResult.type) {
                DecisionType.BUY -> Direction.BUY
                DecisionType.SELL -> Direction.SELL
                DecisionType.WAIT -> Direction.WAIT
            }

        val reasonCodes =
            buildReasonCodes(analysis)

        val createdAt = now

        val expiresAt =
            createdAt + SourceTimeframe.M15.approxLifetimeMillis

        return Decision(
            decisionId = UUID.randomUUID().toString(),
            parentDecisionId = parentDecisionId,
            symbol = snapshot.symbol,
            direction = direction,
            createdAt = createdAt,
            expiresAt = expiresAt,
            sourceTimeframe = SourceTimeframe.M15,
            analysisConfidence = decisionResult.confidence,
            marketState = analysis.marketState.name,
            reasonCodes = reasonCodes
        )
    }

    private fun buildReasonCodes(
        analysis: engine.analysis.MarketAnalysisResult
    ): List<ReasonCode> {

        val reasons = mutableListOf<ReasonCode>()

        when (analysis.trend.direction) {
            TrendDirection.UP ->
                reasons.add(ReasonCode.TREND_UP)

            TrendDirection.DOWN ->
                reasons.add(ReasonCode.TREND_DOWN)

            else -> Unit
        }

        when (analysis.location.location) {
            PriceLocation.NEAR_P25 ->
                reasons.add(ReasonCode.PRICE_NEAR_P25)

            PriceLocation.NEAR_P75 ->
                reasons.add(ReasonCode.PRICE_NEAR_P75)

            else -> Unit
        }

        when (analysis.ad.type) {
            ADType.BUY_AD ->
                reasons.add(ReasonCode.BUY_AD_DETECTED)

            ADType.SELL_AD ->
                reasons.add(ReasonCode.SELL_AD_DETECTED)

            else -> Unit
        }

        if (reasons.isEmpty()) {
            reasons.add(ReasonCode.CONFIRMATION_MISSING)
        }

        return reasons
    }
}
