package com.parkgolf.score.ui.settings

import androidx.annotation.StyleRes
import com.parkgolf.score.R

object ThemeCatalog {
    @StyleRes
    fun styleFor(key: ThemeKey): Int = when (key) {
        ThemeKey.GREEN -> R.style.Theme_ParkGolf_Green
        ThemeKey.BLUE -> R.style.Theme_ParkGolf_Blue
        ThemeKey.ORANGE -> R.style.Theme_ParkGolf_Orange
        ThemeKey.PURPLE -> R.style.Theme_ParkGolf_Purple
    }
}
