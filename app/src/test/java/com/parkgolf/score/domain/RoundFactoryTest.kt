package com.parkgolf.score.domain

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.RoundStatus
import org.junit.Test

class RoundFactoryTest {
    private val courseA = RoundFactory.CoursePars("A코스", List(9) { 3 })
    private val courseB = RoundFactory.CoursePars("B코스", listOf(3,3,4,3,3,4,3,3,5))

    @Test fun buildsHoleSequence_withRenumberedHoles() {
        val round = RoundFactory.newRound(
            venueName = "○○파크골프장",
            players = listOf("나", "김철수"),
            courses = listOf(courseA, courseB),
            date = 5L
        )
        assertThat(round.holes).hasSize(18)
        assertThat(round.holes[0].holeNo).isEqualTo(1)
        assertThat(round.holes[9].holeNo).isEqualTo(10)
        assertThat(round.holes[9].courseName).isEqualTo("B코스")
        assertThat(round.holes[17].par).isEqualTo(5)
    }

    @Test fun initialScores_startAtPar_forEveryPlayer() {
        val round = RoundFactory.newRound("V", listOf("나", "철수"), listOf(courseA), 1L)
        assertThat(round.scores).hasSize(2)
        assertThat(round.scores[0]).hasSize(9)
        assertThat(round.scores[0]).containsExactlyElementsIn(List(9) { 3 })
        assertThat(round.status).isEqualTo(RoundStatus.IN_PROGRESS)
    }
}
