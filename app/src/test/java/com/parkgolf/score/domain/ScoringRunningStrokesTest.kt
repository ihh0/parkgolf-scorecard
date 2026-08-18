package com.parkgolf.score.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScoringRunningStrokesTest {
    @Test fun sumsUpToAndIncludingIndex() {
        val scores = listOf<Int?>(3, 4, 2, 5)
        assertThat(Scoring.runningStrokes(scores, 0)).isEqualTo(3)
        assertThat(Scoring.runningStrokes(scores, 2)).isEqualTo(9)
        assertThat(Scoring.runningStrokes(scores, 3)).isEqualTo(14)
    }

    @Test fun ignoresNulls() {
        val scores = listOf<Int?>(3, null, 4)
        assertThat(Scoring.runningStrokes(scores, 2)).isEqualTo(7)
    }

    @Test fun negativeIndexIsZero() {
        assertThat(Scoring.runningStrokes(listOf<Int?>(3, 4), -1)).isEqualTo(0)
    }

    @Test fun indexBeyondSizeCapsAtList() {
        assertThat(Scoring.runningStrokes(listOf<Int?>(3, 4), 99)).isEqualTo(7)
    }
}
