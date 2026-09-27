package engine.analysis

import engine.data.Candle
import engine.data.MarketSnapshot

enum class PriceLocation {
    PREMIUM,
    DISCOUNT,
    EQUILIBRIUM,
    NEAR_P25,
    NEAR_P75,
    UNCERTAIN
}

data class LocationResult(
    val location: PriceLocation,
    val strength: Int
) {
    init {
        require(strength in 0..100) {
            "strength must be between 0 and 100"
        }
    }
}

object LocationAnalyzer {

    fun analyze(snapshot: MarketSnapshot): LocationResult {

        val h4 = snapshot.h4
        val h1 = snapshot.h1

        if (h4.isEmpty() || h1.isEmpty()) {
            return LocationResult(
                PriceLocation.UNCERTAIN,
                0
            )
        }

        val h4High = h4.maxOf { it.high }
        val h4Low = h4.minOf { it.low }
        val h1High = h1.maxOf { it.high }
        val h1Low = h1.minOf { it.low }

        val h4Range = h4High - h4Low
        val h1Range = h1High - h1Low

        if (h4Range <= 0.0 || h1Range <= 0.0) {
            return LocationResult(
                PriceLocation.UNCERTAIN,
                0
            )
        }

        val currentPrice = h1.last().close

        val positionInH4 =
            (currentPrice - h4Low) / h4Range

        val positionInH1 =
            (currentPrice - h1Low) / h1Range

        val averagePosition =
            (positionInH4 + positionInH1) / 2.0

        val location = when {
            averagePosition >= 0.75 -> PriceLocation.PREMIUM
            averagePosition <= 0.25 -> PriceLocation.DISCOUNT
            averagePosition in 0.45..0.55 -> PriceLocation.EQUILIBRIUM
            else -> PriceLocation.UNCERTAIN
        }

        val strength = when (location) {
            PriceLocation.PREMIUM -> 75
            PriceLocation.DISCOUNT -> 75
            PriceLocation.EQUILIBRIUM -> 50
            PriceLocation.NEAR_P25 -> 60
            PriceLocation.NEAR_P75 -> 60
            PriceLocation.UNCERTAIN -> 20
        }

        return LocationResult(
            location = location,
            strength = strength
        )
    }
}
