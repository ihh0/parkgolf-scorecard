package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import com.parkgolf.score.ui.history.HistoryViewModel
import org.junit.Test

class HistoryViewModelTest {
    private fun round(total: Int, date: Long): Round {
        val a = total / 2
        val b = total - a
        return Round(
            date = date, venueName = "V", players = listOf("나"),
            holes = listOf(HoleSpec("A", 1, 3), HoleSpec("A", 2, 3)),
            scores = listOf(listOf(a, b)), status = RoundStatus.COMPLETED
        )
    }

    @Test fun stats_computeFromPrimaryPlayerTotals() {
        val rounds = listOf(round(38, 300), round(44, 200), round(40, 100))
        val stats = HistoryViewModel.computeStats(rounds, recentN = 10)
        assertThat(stats.recentAverage).isWithin(0.01).of(40.67)
        assertThat(stats.best).isEqualTo(38)
        assertThat(stats.worst).isEqualTo(44)
    }

    @Test fun stats_emptyIsNulls() {
        val stats = HistoryViewModel.computeStats(emptyList(), recentN = 10)
        assertThat(stats.recentAverage).isNull()
        assertThat(stats.best).isNull()
    }
}
