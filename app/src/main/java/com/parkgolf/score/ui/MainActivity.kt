package com.parkgolf.score.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.ui.settings.ThemeCatalog

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeCatalog.styleFor(App.settings(application).themeKey))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
