package com.parkgolf.score.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.ui.hole.HoleInputViewModel
import org.junit.Rule
import org.junit.Test

class HoleInputViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()

    @Test fun navigation_clampsToHoleRange() {
        val vm = HoleInputViewModel(totalHoles = 9)
        assertThat(vm.holeIndex.value).isEqualTo(0)
        vm.prev()
        assertThat(vm.holeIndex.value).isEqualTo(0)
        repeat(20) { vm.next() }
        assertThat(vm.holeIndex.value).isEqualTo(8)
    }

    @Test fun isLastHole_trueOnFinalHole() {
        val vm = HoleInputViewModel(totalHoles = 3)
        assertThat(vm.isLastHole()).isFalse()
        vm.next(); vm.next()
        assertThat(vm.isLastHole()).isTrue()
    }

    @Test fun goTo_jumpsToIndex() {
        val vm = HoleInputViewModel(totalHoles = 9)
        vm.goTo(5)
        assertThat(vm.holeIndex.value).isEqualTo(5)
    }
}
