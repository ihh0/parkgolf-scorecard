package com.parkgolf.score.ui.settings

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("parkgolf_settings", Context.MODE_PRIVATE)

    var themeKey: ThemeKey
        get() = ThemeKey.fromStored(prefs.getString(KEY_THEME, null))
        set(value) { prefs.edit().putString(KEY_THEME, value.stored).apply() }

    var defaultPlayerName: String
        get() = prefs.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
        set(value) { prefs.edit().putString(KEY_NAME, value).apply() }

    companion object {
        const val DEFAULT_NAME = "나"
        private const val KEY_THEME = "theme"
        private const val KEY_NAME = "default_player_name"
    }
}
