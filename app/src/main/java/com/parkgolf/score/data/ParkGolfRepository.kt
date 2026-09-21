package com.parkgolf.score.data

import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.domain.model.Round
import kotlinx.coroutines.flow.Flow

interface ParkGolfRepository {
    // Rounds
    suspend fun saveRound(round: Round): Long
    suspend fun getRound(id: Long): Round?
    fun observeCompletedRounds(): Flow<List<Round>>
    suspend fun completedRounds(): List<Round>
    suspend fun currentInProgressRound(): Round?
    suspend fun deleteRound(id: Long)

    // Venues & courses
    fun observeVenues(): Flow<List<VenueEntity>>
    suspend fun coursesForVenue(venueId: Long): List<CourseEntity>
    suspend fun upsertVenue(venue: VenueEntity): Long
    suspend fun upsertCourse(course: CourseEntity): Long
    suspend fun deleteVenue(venue: VenueEntity)
    suspend fun deleteCourse(course: CourseEntity)
    suspend fun deleteVenueWithCourses(venueId: Long)
    fun observeCourses(): Flow<List<CourseEntity>>
}
