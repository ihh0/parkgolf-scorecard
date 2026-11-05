package com.parkgolf.score.ui.stats

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.StatsPeriod
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import org.junit.Test

class StatsViewModelTest {
    private val day = 24L * 60 * 60 * 1000
    private fun round(dateMillis: Long, course: String, rel: Int): Round {
        val holes = List(9) { HoleSpec(course, it + 1, 3) }
        val mine = MutableList(9) { 3 }.also { it[0] = 3 + rel }
        return Round(dateMillis, dateMillis, "동작", listOf("나"), holes, listOf(mine), RoundStatus.COMPLETED)
    }

    @Test fun compute_defaults_recent10_allCourses() {
        val vm = StatsViewModel()
        val rounds = (1..12).map { round(it * day, "A코스", 0) }
        val state = vm.compute(rounds, nowMillis = 100 * day)
        assertThat(vm.period).isEqualTo(StatsPeriod.RECENT_10)
        assertThat(vm.courseLabel).isNull()
        assertThat(state.graph).hasSize(10)
        assertThat(state.summary.count).isEqualTo(10)
        assertThat(state.courseOptions).containsExactly("동작 A코스")
    }

    @Test fun compute_courseFilter_thenPeriod() {
        val vm = StatsViewModel()
        vm.courseLabel = "동작 B코스"
        vm.period = StatsPeriod.ALL
        val rounds = listOf(
            round(1 * day, "A코스", +2),
            round(2 * day, "B코스", -1),
            round(3 * day, "B코스", +1),
        )
        val state = vm.compute(rounds, nowMillis = 100 * day)
        assertThat(state.graph.map { it.relativeToPar }).containsExactly(-1, 1).inOrder()
        assertThat(state.courseOptions).containsExactly("동작 A코스", "동작 B코스").inOrder()
    }
}
