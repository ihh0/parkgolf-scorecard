package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import com.parkgolf.score.ui.start.StartViewModel
import org.junit.Test

class StartViewModelTest {
    private fun round(venue: String, holes: Int, date: Long) = Round(
        id = 0, date = date, venueName = venue, players = listOf("나"),
        holes = List(holes) { HoleSpec("A", it + 1, 3) },
        scores = listOf(List(holes) { 3 as Int? }), status = RoundStatus.COMPLETED
    )

    @Test fun recentCourses_areDistinctByVenue_newestFirst() {
        val completed = listOf(
            round("○○", 18, 300),
            round("△△", 9, 200),
            round("○○", 9, 100)
        )
        val recents = StartViewModel.deriveRecentCourses(completed, limit = 5)
        assertThat(recents.map { it.venueName }).containsExactly("○○", "△△").inOrder()
        assertThat(recents[0].holeCount).isEqualTo(18)
    }
}
