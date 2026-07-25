package com.parkgolf.score.data.db

import androidx.room.*
import com.parkgolf.score.data.db.entity.RoundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    @Insert suspend fun insert(round: RoundEntity): Long
    @Update suspend fun update(round: RoundEntity)
    @Delete suspend fun delete(round: RoundEntity)
    @Query("SELECT * FROM rounds WHERE id = :id") suspend fun byId(id: Long): RoundEntity?
    @Query("SELECT * FROM rounds WHERE status = 'COMPLETED' ORDER BY date DESC")
    fun observeCompleted(): Flow<List<RoundEntity>>
    @Query("SELECT * FROM rounds WHERE status = 'IN_PROGRESS' ORDER BY date DESC LIMIT 1")
    suspend fun currentInProgress(): RoundEntity?
    @Query("SELECT * FROM rounds WHERE status = 'COMPLETED' ORDER BY date DESC")
    suspend fun completedList(): List<RoundEntity>
}
