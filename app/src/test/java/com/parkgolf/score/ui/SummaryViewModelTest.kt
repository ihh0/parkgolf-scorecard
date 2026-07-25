package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import com.parkgolf.score.ui.summary.SummaryViewModel
import org.junit.Test

class SummaryViewModelTest {
    private val round = Round(
        date = 1L, venueName = "V", players = listOf("나", "철수", "영희"),
        holes = listOf(HoleSpec("A", 1, 3), HoleSpec("A", 2, 3)),
        scores = listOf(listOf(3, 4), listOf(3, 3), listOf(5, 5)),
        status = RoundStatus.IN_PROGRESS
    )

    @Test fun ranking_ordersByTotalAscending() {
        val rows = SummaryViewModel.ranking(round)
        assertThat(rows.map { it.player }).containsExactly("철수", "나", "영희").inOrder()
        assertThat(rows[0].total).isEqualTo(6)
        assertThat(rows[0].rank).isEqualTo(1)
    }

    @Test fun ranking_tiesShareRank() {
        val tie = round.copy(scores = listOf(listOf(3, 3), listOf(3, 3), listOf(5, 5)))
        val rows = SummaryViewModel.ranking(tie)
        assertThat(rows[0].rank).isEqualTo(1)
        assertThat(rows[1].rank).isEqualTo(1)
        assertThat(rows[2].rank).isEqualTo(3)
    }
}
