package com.parkgolf.score.domain.model

/** A single hole as snapshotted into a round: its origin course, hole number, and par. */
data class HoleSpec(
    val courseName: String,
    val holeNo: Int,
    val par: Int
)
