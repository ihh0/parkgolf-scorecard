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
        vm.commitDefaultForHole(0)
        vm.adjust(playerIndex = 0, holeIndex = 0, delta = +1)
        assertThat(vm.round.value!!.scores[0][0]).isEqualTo(4)
    }

    @Test fun cumulativeRelative_reflectsScores() {
        val vm = vmWithRound()
        vm.commitDefaultForHole(0)
        vm.adjust(0, 0, +1)
        assertThat(vm.cumulativeRelative(0)).isEqualTo(1)
    }

    @Test fun commitDefaultForHole_fillsNullsWithPar_onlyThatHole() {
        val vm = vmWithRound()
        val changed = vm.commitDefaultForHole(0)
        assertThat(changed).isTrue()
        assertThat(vm.round.value!!.scores[0][0]).isEqualTo(3)
        assertThat(vm.round.value!!.scores[1][0]).isEqualTo(3)
        assertThat(vm.round.value!!.scores[0][1]).isNull()
    }

    @Test fun commitDefaultForHole_noChange_whenAlreadyFilled() {
        val vm = vmWithRound()
        vm.commitDefaultForHole(0)
        assertThat(vm.commitDefaultForHole(0)).isFalse()
    }

    @Test fun commitDefaultForHole_fillsOnlyNullPlayers_keepsExisting() {
        val vm = vmWithRound()
        // player 0 manually scored at hole 0 (null -> 0+4), player 1 still null
        vm.adjust(0, 0, +4)
        assertThat(vm.round.value!!.scores[0][0]).isEqualTo(4)
        assertThat(vm.round.value!!.scores[1][0]).isNull()
        val changed = vm.commitDefaultForHole(0)
        assertThat(changed).isTrue()
        assertThat(vm.round.value!!.scores[0][0]).isEqualTo(4) // untouched
        assertThat(vm.round.value!!.scores[1][0]).isEqualTo(3) // filled to par
    }

    @Test fun commitDefaultForHole_nonZeroIndex_leavesOtherHolesNull() {
        val vm = vmWithRound()
        val changed = vm.commitDefaultForHole(3)
        assertThat(changed).isTrue()
        assertThat(vm.round.value!!.scores[0][3]).isEqualTo(3)
        assertThat(vm.round.value!!.scores[0][0]).isNull()
    }
}
