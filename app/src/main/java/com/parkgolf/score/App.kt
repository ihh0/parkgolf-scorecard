package com.parkgolf.score

import android.app.Application
import androidx.room.Room
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.data.ParkGolfRepositoryImpl
import com.parkgolf.score.data.db.AppDatabase

class App : Application() {
    lateinit var repository: ParkGolfRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "parkgolf.db").build()
        repository = ParkGolfRepositoryImpl(db.venueDao(), db.courseDao(), db.roundDao())
    }

    companion object {
        fun repo(app: Application): ParkGolfRepository = (app as App).repository
    }
}
