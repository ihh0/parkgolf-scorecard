package com.parkgolf.score.domain

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.ParRelation
import org.junit.Test

class ScoringTest {
    private val holes = listOf(
        HoleSpec("A", 1, 3),
        HoleSpec("A", 2, 3),
        HoleSpec("A", 3, 4)
    )

    @Test fun total_ignoresNulls() {
        assertThat(Scoring.total(listOf(3, null, 5))).isEqualTo(8)
    }

    @Test fun parTotal_sumsPars() {
        assertThat(Scoring.parTotal(holes)).isEqualTo(10)
    }

    @Test fun relativeToPar_countsOnlyPlayedHoles() {
        assertThat(Scoring.relativeToPar(listOf(4, null, 3), holes)).isEqualTo(0)
    }

    @Test fun label_formatsUnderEvenOver() {
        assertThat(Scoring.relationLabel(-2)).isEqualTo("-2")
        assertThat(Scoring.relationLabel(0)).isEqualTo("E")
        assertThat(Scoring.relationLabel(3)).isEqualTo("+3")
    }

    @Test fun relation_classifies() {
        assertThat(Scoring.relation(-1)).isEqualTo(ParRelation.UNDER)
        assertThat(Scoring.relation(0)).isEqualTo(ParRelation.EVEN)
        assertThat(Scoring.relation(2)).isEqualTo(ParRelation.OVER)
    }
}
