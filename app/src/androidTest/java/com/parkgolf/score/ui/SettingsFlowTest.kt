package com.parkgolf.score.ui

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.R
import com.parkgolf.score.ui.settings.SettingsStore
import com.parkgolf.score.ui.settings.ThemeKey
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {
    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    @After fun resetSettings() {
        SettingsStore(ctx).apply { themeKey = ThemeKey.GREEN; defaultPlayerName = SettingsStore.DEFAULT_NAME }
    }

    @Test fun store_persistsThemeAndName() {
        SettingsStore(ctx).apply { themeKey = ThemeKey.BLUE; defaultPlayerName = "홍길동" }
        val reloaded = SettingsStore(ctx)
        assertThat(reloaded.themeKey).isEqualTo(ThemeKey.BLUE)
        assertThat(reloaded.defaultPlayerName).isEqualTo("홍길동")
    }

    @Test fun homeSettingsButton_opensSettings() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.btnSettings)).perform(click())
        onView(withText(R.string.theme_section_title)).check(matches(isDisplayed()))
    }
}
