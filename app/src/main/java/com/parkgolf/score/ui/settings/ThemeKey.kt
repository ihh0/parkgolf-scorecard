package com.parkgolf.score.ui.settings

enum class ThemeKey(val stored: String) {
    GREEN("green"), BLUE("blue"), ORANGE("orange"), PURPLE("purple");

    companion object {
        fun fromStored(s: String?): ThemeKey = entries.firstOrNull { it.stored == s } ?: GREEN
    }
}
