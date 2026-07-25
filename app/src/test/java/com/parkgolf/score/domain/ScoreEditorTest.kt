package com.parkgolf.score.domain

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import org.junit.Test

class ScoreEditorTest {
    private val round = Round(
        date = 1L, venueName = "V", players = listOf("나", "철수"),
        holes = listOf(HoleSpec("A", 1, 3), HoleSpec("A", 2, 3)),
        scores = listOf(mutableListOf<Int?>(3, 3), mutableListOf<Int?>(3, 3)),
        status = RoundStatus.IN_PROGRESS
    )

    @Test fun increment_addsOne() {
        val r = ScoreEditor.adjust(round, playerIndex = 0, holeIndex = 1, delta = +1)
        assertThat(r.scores[0][1]).isEqualTo(4)
        assertThat(r.scores[1][1]).isEqualTo(3)
    }

    @Test fun decrement_clampsAtOne() {
        var r = round
        repeat(5) { r = ScoreEditor.adjust(r, 0, 0, -1) }
        assertThat(r.scores[0][0]).isEqualTo(1)
    }

    @Test fun setScore_setsExactValue() {
        val r = ScoreEditor.set(round, 1, 0, 6)
        assertThat(r.scores[1][0]).isEqualTo(6)
    }

    @Test fun adjust_doesNotMutateOriginal() {
        ScoreEditor.adjust(round, 0, 0, +1)
        assertThat(round.scores[0][0]).isEqualTo(3)
    }
}
