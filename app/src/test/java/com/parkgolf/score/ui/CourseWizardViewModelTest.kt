package com.parkgolf.score.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.ui.courses.CourseWizardViewModel
import org.junit.Rule
import org.junit.Test

class CourseWizardViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()

    private fun vm() = CourseWizardViewModel().apply { initNew("A코스") }

    @Test fun initNew_setsSuggestedCourseNameAndStepZero() {
        val vm = vm()
        assertThat(vm.draft.value!!.courseName).isEqualTo("A코스")
        assertThat(vm.draft.value!!.step).isEqualTo(0)
    }

    @Test fun setHoleCount_resizesPars() {
        val vm = vm()
        vm.setHoleCount(3)
        assertThat(vm.draft.value!!.pars).hasSize(3)
        vm.setHoleCount(12)
        assertThat(vm.draft.value!!.pars).hasSize(12)
    }

    @Test fun setPar_clampsToMinimumOne() {
        val vm = vm()
        vm.setPar(0, -5)
        assertThat(vm.draft.value!!.pars[0]).isEqualTo(1)
    }

    @Test fun next_prev_clampWithinRange() {
        val vm = vm()
        vm.prev()
        assertThat(vm.draft.value!!.step).isEqualTo(0)
        repeat(10) { vm.next() }
        assertThat(vm.draft.value!!.step).isEqualTo(3)
    }

    @Test fun canAdvance_reflectsCurrentStepValidity() {
        val vm = vm()
        vm.setVenueName("")
        assertThat(vm.canAdvance()).isFalse()
        vm.setVenueName("한강")
        assertThat(vm.canAdvance()).isTrue()
    }

    @Test fun loadForEdit_populatesDraft() {
        val vm = CourseWizardViewModel()
        vm.loadForEdit(venueId = 5, courseId = 8, venueName = "한강", courseName = "B코스", pars = listOf(3, 4, 5))
        val d = vm.draft.value!!
        assertThat(d.editingVenueId).isEqualTo(5)
        assertThat(d.editingCourseId).isEqualTo(8)
        assertThat(d.holeCount).isEqualTo(3)
        assertThat(d.pars).containsExactly(3, 4, 5).inOrder()
    }
}
