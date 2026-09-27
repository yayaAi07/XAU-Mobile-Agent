package engine.setup

import engine.analysis.ADResult
import engine.analysis.ADType
import engine.analysis.LocationResult
import engine.analysis.MarketAnalysisResult
import engine.analysis.MarketState
import engine.analysis.PriceLocation
import engine.analysis.StructureDirection
import engine.analysis.StructureResult
import engine.analysis.TrendDirection
import engine.analysis.TrendResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupEngineTest {

    @Test
    fun buySetup_isCreated_whenAllBuyConditionsAreMet() {

        val analysis = createAnalysis(
            trendDirection = TrendDirection.UP,
            trendStrength = 90,
            structureDirection = StructureDirection.BULLISH,
            structureStrength = 90,
            location = PriceLocation.DISCOUNT,
            locationStrength = 80,
            adType = ADType.BUY_AD,
            adStrength = 90,
            marketState = MarketState.TREND_UP
        )

        val setup = SetupEngine.build(
            analysis = analysis,
            entryPrice = 2000.0
        )

        assertEquals(SetupDirection.BUY, setup.direction)
        assertTrue(setup.stopLoss < setup.entryPrice)
        assertTrue(setup.takeProfit > setup.entryPrice)
        assertTrue(setup.confidence >= 60)
    }

    @Test
    fun sellSetup_isCreated_whenAllSellConditionsAreMet() {

        val analysis = createAnalysis(
            trendDirection = TrendDirection.DOWN,
            trendStrength = 90,
            structureDirection = StructureDirection.BEARISH,
            structureStrength = 90,
            location = PriceLocation.PREMIUM,
            locationStrength = 80,
            adType = ADType.SELL_AD,
            adStrength = 90,
            marketState = MarketState.TREND_DOWN
        )

        val setup = SetupEngine.build(
            analysis = analysis,
            entryPrice = 2000.0
        )

        assertEquals(SetupDirection.SELL, setup.direction)
        assertTrue(setup.stopLoss > setup.entryPrice)
        assertTrue(setup.takeProfit < setup.entryPrice)
        assertTrue(setup.confidence >= 60)
    }

    @Test
    fun setupIsNone_whenBuyConfluenceIsIncomplete() {

        val analysis = createAnalysis(
            trendDirection = TrendDirection.UP,
            trendStrength = 90,
            structureDirection = StructureDirection.BULLISH,
            structureStrength = 90,
            location = PriceLocation.DISCOUNT,
            locationStrength = 80,
            adType = ADType.NONE,
            adStrength = 0,
            marketState = MarketState.TREND_UP
        )

        val setup = SetupEngine.build(
            analysis = analysis,
            entryPrice = 2000.0
        )

        assertEquals(SetupDirection.NONE, setup.direction)
        assertEquals(0, setup.confidence)
    }

    @Test
    fun setupIsNone_whenEntryPriceIsInvalid() {

        val analysis = createAnalysis(
            trendDirection = TrendDirection.UP,
            trendStrength = 90,
            structureDirection = StructureDirection.BULLISH,
            structureStrength = 90,
            location = PriceLocation.DISCOUNT,
            locationStrength = 80,
            adType = ADType.BUY_AD,
            adStrength = 90,
            marketState = MarketState.TREND_UP
        )

        val setup = SetupEngine.build(
            analysis = analysis,
            entryPrice = 0.0
        )

        assertEquals(SetupDirection.NONE, setup.direction)
        assertEquals(0, setup.confidence)
    }

    @Test
    fun setupIsNone_whenConfidenceIsBelowMinimum() {

        val analysis = createAnalysis(
            trendDirection = TrendDirection.UP,
            trendStrength = 60,
            structureDirection = StructureDirection.BULLISH,
            structureStrength = 60,
            location = PriceLocation.DISCOUNT,
            locationStrength = 60,
            adType = ADType.BUY_AD,
            adStrength = 60,
            marketState = MarketState.TREND_UP
        )

        val setup = SetupEngine.build(
            analysis = analysis,
            entryPrice = 2000.0
        )

        assertEquals(SetupDirection.NONE, setup.direction)
        assertEquals(0, setup.confidence)
    }

    private fun createAnalysis(
        trendDirection: TrendDirection,
        trendStrength: Int,
        structureDirection: StructureDirection,
        structureStrength: Int,
        location: PriceLocation,
        locationStrength: Int,
        adType: ADType,
        adStrength: Int,
        marketState: MarketState
    ): MarketAnalysisResult {

        return MarketAnalysisResult(
            marketState = marketState,

            trend = TrendResult(
                direction = trendDirection,
                strength = trendStrength
            ),

            structure = StructureResult(
                direction = structureDirection,
                hasBreakOfStructure = false,
                hasChangeOfCharacter = false,
                strength = structureStrength
            ),

            location = LocationResult(
                location = location,
                strength = locationStrength
            ),

            ad = ADResult(
                type = adType,
                timeframe = "H1",
                strength = adStrength
            ),

            overallStrength = (
                trendStrength * 0.30 +
                structureStrength * 0.30 +
                locationStrength * 0.20 +
                adStrength * 0.20
            ).toInt()
        )
    }
}
