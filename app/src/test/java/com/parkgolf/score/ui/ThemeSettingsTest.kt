package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.ui.settings.ThemeKey
import org.junit.Test

class ThemeSettingsTest {
    @Test fun fromStored_roundTrips() {
        ThemeKey.entries.forEach { key ->
            assertThat(ThemeKey.fromStored(key.stored)).isEqualTo(key)
        }
    }

    @Test fun fromStored_unknownDefaultsToGreen() {
        assertThat(ThemeKey.fromStored(null)).isEqualTo(ThemeKey.GREEN)
        assertThat(ThemeKey.fromStored("teal")).isEqualTo(ThemeKey.GREEN)
    }
}
