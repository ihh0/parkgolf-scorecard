package com.parkgolf.score.data

import com.parkgolf.score.data.db.CourseDao
import com.parkgolf.score.data.db.RoundDao
import com.parkgolf.score.data.db.VenueDao
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.domain.model.Round
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ParkGolfRepositoryImpl(
    private val venueDao: VenueDao,
    private val courseDao: CourseDao,
    private val roundDao: RoundDao
) : ParkGolfRepository {

    override suspend fun saveRound(round: Round): Long {
        val entity = round.toEntity()
        return if (round.id == 0L) roundDao.insert(entity)
        else { roundDao.update(entity); round.id }
    }

    override suspend fun getRound(id: Long): Round? = roundDao.byId(id)?.toDomain()

    override fun observeCompletedRounds(): Flow<List<Round>> =
        roundDao.observeCompleted().map { list -> list.map { it.toDomain() } }

    override suspend fun completedRounds(): List<Round> =
        roundDao.completedList().map { it.toDomain() }

    override suspend fun currentInProgressRound(): Round? =
        roundDao.currentInProgress()?.toDomain()

    override suspend fun deleteRound(id: Long) {
        roundDao.byId(id)?.let { roundDao.delete(it) }
    }

    override fun observeVenues(): Flow<List<VenueEntity>> = venueDao.observeAll()
    override suspend fun coursesForVenue(venueId: Long): List<CourseEntity> =
        courseDao.forVenue(venueId)

    override suspend fun upsertVenue(venue: VenueEntity): Long =
        if (venue.id == 0L) venueDao.insert(venue) else { venueDao.update(venue); venue.id }

    override suspend fun upsertCourse(course: CourseEntity): Long =
        if (course.id == 0L) courseDao.insert(course) else { courseDao.update(course); course.id }

    override suspend fun deleteVenue(venue: VenueEntity) = venueDao.delete(venue)
    override suspend fun deleteCourse(course: CourseEntity) = courseDao.delete(course)
    override fun observeCourses(): Flow<List<CourseEntity>> = courseDao.observeAll()
}
