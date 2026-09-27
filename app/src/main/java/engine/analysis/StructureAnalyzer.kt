package engine.analysis

import engine.data.Candle
import engine.data.MarketSnapshot

enum class StructureDirection {
    BULLISH,
    BEARISH,
    NEUTRAL,
    UNCERTAIN
}

data class StructureResult(
    val direction: StructureDirection,
    val hasBreakOfStructure: Boolean,
    val hasChangeOfCharacter: Boolean,
    val strength: Int
) {
    init {
        require(strength in 0..100) {
            "strength must be between 0 and 100"
        }
    }
}

object StructureAnalyzer {

    private const val SWING_LEFT = 2
    private const val SWING_RIGHT = 2

    fun analyze(snapshot: MarketSnapshot): StructureResult {

        val h1 = snapshot.h1
        val m15 = snapshot.m15

        if (
            h1.size < SWING_LEFT + SWING_RIGHT + 3 ||
            m15.size < SWING_LEFT + SWING_RIGHT + 3
        ) {
            return StructureResult(
                direction = StructureDirection.UNCERTAIN,
                hasBreakOfStructure = false,
                hasChangeOfCharacter = false,
                strength = 0
            )
        }

        val h1Structure = analyzeTimeframe(h1)
        val m15Structure = analyzeTimeframe(m15)

        val direction = combineDirection(
            h1Structure.direction,
            m15Structure.direction
        )

        val hasBos =
            h1Structure.hasBreakOfStructure ||
            m15Structure.hasBreakOfStructure

        val hasChoch =
            h1Structure.hasChangeOfCharacter ||
            m15Structure.hasChangeOfCharacter

        val strength = when {
            direction == StructureDirection.UNCERTAIN -> 30
            hasBos && hasChoch -> 80
            hasBos -> 70
            hasChoch -> 50
            direction == StructureDirection.NEUTRAL -> 25
            else -> 55
        }

        return StructureResult(
            direction = direction,
            hasBreakOfStructure = hasBos,
            hasChangeOfCharacter = hasChoch,
            strength = strength
        )
    }

    private data class SwingPoint(
        val index: Int,
        val price: Double
    )

    private data class TimeframeStructure(
        val direction: StructureDirection,
        val hasBreakOfStructure: Boolean,
        val hasChangeOfCharacter: Boolean
    )

    private fun analyzeTimeframe(
        candles: List<Candle>
    ): TimeframeStructure {

        val swingHighs = findSwingHighs(candles)
        val swingLows = findSwingLows(candles)

        if (swingHighs.size < 2 || swingLows.size < 2) {
            return TimeframeStructure(
                direction = StructureDirection.UNCERTAIN,
                hasBreakOfStructure = false,
                hasChangeOfCharacter = false
            )
        }

        val previousHigh = swingHighs[swingHighs.size - 2]
        val latestHigh = swingHighs.last()

        val previousLow = swingLows[swingLows.size - 2]
        val latestLow = swingLows.last()

        val bullishStructure =
            latestHigh.price > previousHigh.price &&
            latestLow.price > previousLow.price

        val bearishStructure =
            latestHigh.price < previousHigh.price &&
            latestLow.price < previousLow.price

        val direction = when {
            bullishStructure -> StructureDirection.BULLISH
            bearishStructure -> StructureDirection.BEARISH
            else -> StructureDirection.NEUTRAL
        }

        val lastClose = candles.last().close

        val bullishBos =
            lastClose > latestHigh.price

        val bearishBos =
            lastClose < latestLow.price

        val hasBos =
            bullishBos || bearishBos

        val hasChoch =
            (direction == StructureDirection.BEARISH && bullishBos) ||
            (direction == StructureDirection.BULLISH && bearishBos)

        return TimeframeStructure(
            direction = direction,
            hasBreakOfStructure = hasBos,
            hasChangeOfCharacter = hasChoch
        )
    }

    private fun findSwingHighs(
        candles: List<Candle>
    ): List<SwingPoint> {

        val result = mutableListOf<SwingPoint>()

        for (
            i in SWING_LEFT until
                candles.size - SWING_RIGHT
        ) {

            val current = candles[i].high

            var isSwingHigh = true

            for (j in 1..SWING_LEFT) {
                if (current <= candles[i - j].high) {
                    isSwingHigh = false
                    break
                }
            }

            if (!isSwingHigh) {
                continue
            }

            for (j in 1..SWING_RIGHT) {
                if (current <= candles[i + j].high) {
                    isSwingHigh = false
                    break
                }
            }

            if (isSwingHigh) {
                result.add(
                    SwingPoint(
                        index = i,
                        price = current
                    )
                )
            }
        }

        return result
    }

    private fun findSwingLows(
        candles: List<Candle>
    ): List<SwingPoint> {

        val result = mutableListOf<SwingPoint>()

        for (
            i in SWING_LEFT until
                candles.size - SWING_RIGHT
        ) {

            val current = candles[i].low

            var isSwingLow = true

            for (j in 1..SWING_LEFT) {
                if (current >= candles[i - j].low) {
                    isSwingLow = false
                    break
                }
            }

            if (!isSwingLow) {
                continue
            }

            for (j in 1..SWING_RIGHT) {
                if (current >= candles[i + j].low) {
                    isSwingLow = false
                    break
                }
            }

            if (isSwingLow) {
                result.add(
                    SwingPoint(
                        index = i,
                        price = current
                    )
                )
            }
        }

        return result
    }

    private fun combineDirection(
        h1: StructureDirection,
        m15: StructureDirection
    ): StructureDirection {

        return when {
            h1 == StructureDirection.BULLISH &&
                m15 == StructureDirection.BULLISH ->
                StructureDirection.BULLISH

            h1 == StructureDirection.BEARISH &&
                m15 == StructureDirection.BEARISH ->
                StructureDirection.BEARISH

            h1 == StructureDirection.NEUTRAL &&
                m15 == StructureDirection.NEUTRAL ->
                StructureDirection.NEUTRAL

            else ->
                StructureDirection.UNCERTAIN
        }
    }
}
