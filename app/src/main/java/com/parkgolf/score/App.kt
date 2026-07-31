package com.parkgolf.score

import android.app.Application
import androidx.room.Room
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.data.ParkGolfRepositoryImpl
import com.parkgolf.score.data.db.AppDatabase
import com.parkgolf.score.ui.settings.SettingsStore

class App : Application() {
    lateinit var repository: ParkGolfRepository
        private set
    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "parkgolf.db").build()
        repository = ParkGolfRepositoryImpl(db.venueDao(), db.courseDao(), db.roundDao())
        settings = SettingsStore(this)
    }

    companion object {
        fun repo(app: Application): ParkGolfRepository = (app as App).repository
        fun settings(app: Application): SettingsStore = (app as App).settings
    }
}
