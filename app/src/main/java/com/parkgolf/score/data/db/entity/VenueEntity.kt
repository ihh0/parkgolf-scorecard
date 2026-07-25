package com.parkgolf.score.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "venues")
data class VenueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val memo: String? = null
)
