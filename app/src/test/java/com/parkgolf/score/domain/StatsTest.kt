package com.parkgolf.score.domain

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
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

    private fun roundRel(dateMillis: Long, course: String, rel: Int, holeCount: Int = 9): Round {
        val holes = List(holeCount) { HoleSpec(course, it + 1, 3) }
        val mine = MutableList(holeCount) { 3 }
        mine[0] = 3 + rel
        return Round(
            id = dateMillis, date = dateMillis, venueName = "동작",
            players = listOf("나"), holes = holes,
            scores = listOf(mine), status = RoundStatus.COMPLETED
        )
    }

    private val day = 24L * 60 * 60 * 1000

    @Test fun relativeScores_sortedByDate_ascending_withRel() {
        val rounds = listOf(
            roundRel(3 * day, "A코스", +2),
            roundRel(1 * day, "A코스", -1),
            roundRel(2 * day, "B코스", 0),
        )
        val out = Stats.relativeScores(rounds)
        assertThat(out.map { it.date }).containsExactly(1 * day, 2 * day, 3 * day).inOrder()
        assertThat(out.map { it.relativeToPar }).containsExactly(-1, 0, 2).inOrder()
        assertThat(out[0].courseLabel).isEqualTo("동작 A코스")
    }

    @Test fun filterByCourse_nullIsAll_elseExact() {
        val all = Stats.relativeScores(listOf(roundRel(1 * day, "A코스", 1), roundRel(2 * day, "B코스", 2)))
        assertThat(Stats.filterByCourse(all, null)).hasSize(2)
        assertThat(Stats.filterByCourse(all, "동작 A코스").map { it.relativeToPar }).containsExactly(1)
    }

    @Test fun filterByPeriod_recent10_takesLastTen() {
        val all = Stats.relativeScores((1..15).map { roundRel(it * day, "A코스", 0) })
        val recent = Stats.filterByPeriod(all, StatsPeriod.RECENT_10, nowMillis = 100 * day)
        assertThat(recent).hasSize(10)
        assertThat(recent.first().date).isEqualTo(6 * day)
        assertThat(recent.last().date).isEqualTo(15 * day)
    }

    @Test fun filterByPeriod_threeMonths_boundaryInclusive() {
        val now = 365L * day
        val all = Stats.relativeScores(listOf(
            roundRel(now - 200 * day, "A코스", 0),
            roundRel(now - 10 * day, "A코스", 1),
        ))
        val within = Stats.filterByPeriod(all, StatsPeriod.THREE_MONTHS, nowMillis = now)
        assertThat(within.map { it.relativeToPar }).containsExactly(1)
    }

    @Test fun filterByPeriod_all_returnsAll() {
        val all = Stats.relativeScores((1..5).map { roundRel(it * day, "A코스", 0) })
        assertThat(Stats.filterByPeriod(all, StatsPeriod.ALL, nowMillis = 0)).hasSize(5)
    }

    @Test fun summarize_countAvgBestWorst() {
        val all = Stats.relativeScores(listOf(
            roundRel(1 * day, "A코스", -1),
            roundRel(2 * day, "A코스", +3),
            roundRel(3 * day, "A코스", +1),
        ))
        val s = Stats.summarize(all)
        assertThat(s.count).isEqualTo(3)
        assertThat(s.average!!).isWithin(0.01).of(1.0)
        assertThat(s.best).isEqualTo(-1)
        assertThat(s.worst).isEqualTo(3)
    }

    @Test fun summarize_emptyIsZeroAndNulls() {
        val s = Stats.summarize(emptyList())
        assertThat(s.count).isEqualTo(0)
        assertThat(s.average).isNull()
        assertThat(s.best).isNull()
        assertThat(s.worst).isNull()
    }

    @Test fun distinctCourseLabels_preserveOrder() {
        val all = Stats.relativeScores(listOf(
            roundRel(1 * day, "A코스", 0),
            roundRel(2 * day, "B코스", 0),
            roundRel(3 * day, "A코스", 0),
        ))
        assertThat(Stats.distinctCourseLabels(all)).containsExactly("동작 A코스", "동작 B코스").inOrder()
    }
}
