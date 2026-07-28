package com.parkgolf.score.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parkgolf.score.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourseWizardFlowTest {

    @Test fun createCourse_throughAllSteps_savesAndReturns() {
        ActivityScenario.launch(MainActivity::class.java)
        // Home -> 내 구장 관리
        onView(withId(R.id.btnMyCourses)).perform(click())
        // + 새 구장 만들기 -> wizard
        onView(withId(R.id.btnAddVenue)).perform(click())
        // Step 0: venue name
        onView(withId(R.id.etVenueName)).perform(replaceText("테스트구장"), closeSoftKeyboard())
        onView(withId(R.id.btnNext)).perform(click())
        // Step 1: course name (default A코스)
        onView(withId(R.id.btnNext)).perform(click())
        // Step 2: hole setup
        onView(withId(R.id.btnNext)).perform(click())
        // Step 3: save
        onView(withId(R.id.btnNext)).perform(click())
        // Back on the list, the new venue appears
        onView(withText("테스트구장")).check(matches(isDisplayed()))
    }

    @Test fun backOnFirstStep_showsCancelDialog() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.btnMyCourses)).perform(click())
        onView(withId(R.id.btnAddVenue)).perform(click())
        onView(withId(R.id.etVenueName)).perform(replaceText("취소구장"), closeSoftKeyboard())
        // Tap the top-bar back button
        onView(withId(R.id.btnBack)).perform(click())
        onView(withText(R.string.confirm_cancel_create)).check(matches(isDisplayed()))
    }
}
