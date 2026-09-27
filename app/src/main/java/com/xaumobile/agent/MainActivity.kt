package com.xaumobile.agent

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import engine.data.MarketDataRepository
import engine.data.MockMarketDataSource
import engine.decision.DecisionAnalysisAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var decisionText: TextView
    private lateinit var confidenceText: TextView
    private lateinit var marketStateText: TextView
    private lateinit var reasonsText: TextView

    private val repository =
        MarketDataRepository(
            MockMarketDataSource()
        )

    private val analysisAdapter =
        DecisionAnalysisAdapter(
            repository = repository,
            symbol = "XAUUSD"
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildInterface()
    }

    private fun buildInterface() {

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(
                    32,
                    32,
                    32,
                    32
                )
            }

        val title =
            TextView(this).apply {
                text = "XAU Mobile Agent"
                textSize = 26f
                gravity = Gravity.CENTER
            }

        val symbol =
            TextView(this).apply {
                text = "Symbol: XAUUSD"
                textSize = 18f
            }

        statusText =
            TextView(this).apply {
                text = "Status: READY"
                textSize = 18f
            }

        decisionText =
            TextView(this).apply {
                text = "Decision: WAIT"
                textSize = 20f
            }

        confidenceText =
            TextView(this).apply {
                text = "Confidence: 0"
                textSize = 18f
            }

        marketStateText =
            TextView(this).apply {
                text = "Market State: -"
                textSize = 18f
            }

        reasonsText =
            TextView(this).apply {
                text = "Reasons: -"
                textSize = 16f
            }

        val analyzeButton =
            Button(this).apply {
                text = "ANALYZE"

                setOnClickListener {
                    runAnalysis()
                }
            }

        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(symbol)
        root.addView(statusText)
        root.addView(decisionText)
        root.addView(confidenceText)
        root.addView(marketStateText)
        root.addView(reasonsText)

        root.addView(
            analyzeButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)
    }

    private fun runAnalysis() {

        statusText.text = "Status: ANALYZING..."

        val now =
            System.currentTimeMillis()

        try {

            val decision =
                analysisAdapter.analyze(now)

            if (decision == null) {

                statusText.text = "Status: WAIT"

                decisionText.text =
                    "Decision: WAIT"

                confidenceText.text =
                    "Confidence: 0"

                marketStateText.text =
                    "Market State: NO SETUP"

                reasonsText.text =
                    "Reasons: No valid setup"

                return
            }

            statusText.text =
                "Status: DECISION CREATED"

            decisionText.text =
                "Decision: ${decision.direction}"

            confidenceText.text =
                "Confidence: ${decision.analysisConfidence}%"

            marketStateText.text =
                "Market State: ${decision.marketState}"

            reasonsText.text =
                "Reasons:\n" +
                    decision.reasonCodes.joinToString(
                        separator = "\n"
                    ) {
                        "• $it"
                    }

        } catch (error: Exception) {

            statusText.text =
                "Status: ERROR"

            decisionText.text =
                "Decision: WAIT"

            confidenceText.text =
                "Confidence: 0"

            marketStateText.text =
                "Market State: ERROR"

            reasonsText.text =
                "Error: ${error.message}"
        }
    }
}
