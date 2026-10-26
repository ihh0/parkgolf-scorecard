package com.parkgolf.score.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.ui.settings.SettingsStore
import com.parkgolf.score.ui.settings.ThemeCatalog

class MainActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        // 앱 내 글꼴 배율을 절대값으로 적용(시스템 글꼴 설정 대신).
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = SettingsStore(newBase).fontScaleKey.multiplier
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeCatalog.styleFor(App.settings(application).themeKey))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
