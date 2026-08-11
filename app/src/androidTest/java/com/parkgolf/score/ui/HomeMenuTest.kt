package com.parkgolf.score.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parkgolf.score.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeMenuTest {
    @Test fun home_showsThreeMenuItems() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withText(R.string.menu_game_start)).check(matches(isDisplayed()))
        onView(withText(R.string.menu_game_records)).check(matches(isDisplayed()))
        onView(withText(R.string.menu_course_management)).check(matches(isDisplayed()))
    }
}
