package com.parkgolf.score.data.db

import androidx.room.*
import com.parkgolf.score.data.db.entity.VenueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VenueDao {
    @Insert suspend fun insert(venue: VenueEntity): Long
    @Update suspend fun update(venue: VenueEntity)
    @Delete suspend fun delete(venue: VenueEntity)
    @Query("SELECT * FROM venues ORDER BY name") fun observeAll(): Flow<List<VenueEntity>>
    @Query("SELECT * FROM venues WHERE id = :id") suspend fun byId(id: Long): VenueEntity?
}
