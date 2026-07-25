package com.parkgolf.score.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.RoundFactory
import org.junit.Rule
import org.junit.Test

class RoundSessionViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()

    private fun vmWithRound(): RoundSessionViewModel {
        val vm = RoundSessionViewModel()
        vm.startRound(
            RoundFactory.newRound(
                "V", listOf("나", "철수"),
                listOf(RoundFactory.CoursePars("A", List(9) { 3 })), 1L
            )
        )
        return vm
    }

    @Test fun adjustScore_updatesState() {
        val vm = vmWithRound()
        vm.adjust(playerIndex = 0, holeIndex = 0, delta = +1)
        assertThat(vm.round.value!!.scores[0][0]).isEqualTo(4)
    }

    @Test fun cumulativeRelative_reflectsScores() {
        val vm = vmWithRound()
        vm.adjust(0, 0, +1)
        assertThat(vm.cumulativeRelative(0)).isEqualTo(1)
    }
}
