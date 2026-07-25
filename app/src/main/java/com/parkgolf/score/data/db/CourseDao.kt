package com.parkgolf.score.data.db

import androidx.room.*
import com.parkgolf.score.data.db.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Insert suspend fun insert(course: CourseEntity): Long
    @Update suspend fun update(course: CourseEntity)
    @Delete suspend fun delete(course: CourseEntity)
    @Query("SELECT * FROM courses WHERE venueId = :venueId ORDER BY name")
    suspend fun forVenue(venueId: Long): List<CourseEntity>
    @Query("SELECT * FROM courses ORDER BY name") fun observeAll(): Flow<List<CourseEntity>>
}
