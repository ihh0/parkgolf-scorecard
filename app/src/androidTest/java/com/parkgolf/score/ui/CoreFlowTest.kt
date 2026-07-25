package com.parkgolf.score.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parkgolf.score.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreFlowTest {
    @Test fun home_showsStartButton() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.btnStart))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.start_round)))
    }

    @Test fun startButton_navigatesToStartScreen() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.btnStart)).perform(click())
        onView(withId(R.id.btnNewCourse)).check(matches(isDisplayed()))
    }
}
