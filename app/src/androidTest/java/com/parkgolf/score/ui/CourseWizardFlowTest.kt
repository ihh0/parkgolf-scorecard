package com.parkgolf.score.ui

import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parkgolf.score.R
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourseWizardFlowTest {

    /**
     * Scrolls the holes-step [NestedScrollView] to the bottom and clicks the
     * "add hole" button in the same action.
     *
     * The holes step nests a RecyclerView inside a NestedScrollView, which makes
     * the add-hole button sit below the fold. Espresso's scrollTo() cannot reveal
     * it reliably, and doing the scroll and the click as two separate onView()
     * interactions fails because the NestedScrollView's scroll position resets to
     * the top between interactions. Performing both here keeps the button on screen
     * for the click and drives the production click handler directly.
     */
    private fun scrollToAndAddHole(): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(NestedScrollView::class.java)
        override fun getDescription(): String = "scroll holes step to bottom and add a hole"
        override fun perform(uiController: UiController, view: View) {
            val nsv = view as NestedScrollView
            nsv.fullScroll(View.FOCUS_DOWN)
            uiController.loopMainThreadUntilIdle()
            nsv.findViewById<View>(R.id.btnAddHole).performClick()
            uiController.loopMainThreadUntilIdle()
        }
    }

    @Test fun createCourse_throughAllSteps_showsSuccessScreen() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.itemCourseManagement)).perform(click())
        onView(withId(R.id.btnAddVenue)).perform(click())
        onView(withId(R.id.etVenueName)).perform(replaceText("테스트구장"), closeSoftKeyboard())
        onView(withId(R.id.btnNext)).perform(click())
        onView(withId(R.id.btnNext)).perform(click())
        onView(withId(R.id.stepHoles)).perform(scrollToAndAddHole())
        onView(withId(R.id.btnNext)).perform(click())
        onView(withId(R.id.btnNext)).perform(click())
        onView(withText(R.string.save_done_title)).check(matches(isDisplayed()))
    }

    @Test fun backOnFirstStep_showsCancelDialog() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.itemCourseManagement)).perform(click())
        onView(withId(R.id.btnAddVenue)).perform(click())
        onView(withId(R.id.etVenueName)).perform(replaceText("취소구장"), closeSoftKeyboard())
        onView(withId(R.id.btnBack)).perform(click())
        onView(withText(R.string.confirm_cancel_create)).check(matches(isDisplayed()))
    }
}
