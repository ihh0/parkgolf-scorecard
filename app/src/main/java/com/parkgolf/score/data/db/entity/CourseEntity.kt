package com.parkgolf.score.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** pars stored as JSON via Converters (List<Int>). A course is usually 9 holes. */
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val venueId: Long,
    val name: String,
    val pars: List<Int>
)
