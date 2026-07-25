package com.parkgolf.score.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StatsTest {
    private val totals = listOf(41, 39, 45, 38, 40, 42, 44, 37, 43, 40, 99)

    @Test fun recentAverage_usesLastN() {
        val avg = Stats.recentAverageStrokes(totals, 10)
        assertThat(avg).isWithin(0.01).of(40.9)
    }

    @Test fun recentAverage_handlesFewerThanN() {
        assertThat(Stats.recentAverageStrokes(listOf(40, 42), 10)).isWithin(0.01).of(41.0)
    }

    @Test fun recentAverage_emptyIsNull() {
        assertThat(Stats.recentAverageStrokes(emptyList(), 10)).isNull()
    }

    @Test fun best_isLowest_worst_isHighest() {
        assertThat(Stats.best(totals)).isEqualTo(37)
        assertThat(Stats.worst(totals)).isEqualTo(99)
    }
}
