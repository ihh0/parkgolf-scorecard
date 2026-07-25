package com.parkgolf.score.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.parkgolf.score.domain.model.HoleSpec

@Entity(tableName = "rounds")
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val venueName: String,
    val players: List<String>,
    val holes: List<HoleSpec>,
    val scores: List<List<Int?>>,
    val status: String   // "IN_PROGRESS" | "COMPLETED"
)
