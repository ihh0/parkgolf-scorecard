package com.parkgolf.score.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.RoundEntity
import com.parkgolf.score.data.db.entity.VenueEntity

@Database(
    entities = [VenueEntity::class, CourseEntity::class, RoundEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun venueDao(): VenueDao
    abstract fun courseDao(): CourseDao
    abstract fun roundDao(): RoundDao
}
