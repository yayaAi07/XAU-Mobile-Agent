package com.xaumobile.agent

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Test
    fun analyzeButton_runsWithoutCrash() {

        ActivityScenario.launch(
            MainActivity::class.java
        ).use {

            onView(
                withText("ANALYZE")
            ).perform(click())
        }
    }
}
