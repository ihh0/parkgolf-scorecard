package com.parkgolf.score.domain

import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus

object RoundFactory {
    data class CoursePars(val courseName: String, val pars: List<Int>)

    fun newRound(
        venueName: String,
        players: List<String>,
        courses: List<CoursePars>,
        date: Long
    ): Round {
        val holes = mutableListOf<HoleSpec>()
        var holeNo = 1
        for (course in courses) {
            for (par in course.pars) {
                holes += HoleSpec(course.courseName, holeNo, par)
                holeNo++
            }
        }
        val scores = players.map { holes.map { h -> h.par as Int? } }
        return Round(
            date = date,
            venueName = venueName,
            players = players,
            holes = holes,
            scores = scores,
            status = RoundStatus.IN_PROGRESS
        )
    }
}
