package com.parkgolf.score.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.ui.courses.CourseWizardViewModel
import org.junit.Rule
import org.junit.Test

class CourseWizardViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()

    private fun vm() = CourseWizardViewModel().apply { initNew("A코스") }

    @Test fun initNew_setsCourseNameStepZeroNineHoles() {
        val vm = vm()
        assertThat(vm.draft.value!!.courseName).isEqualTo("A코스")
        assertThat(vm.draft.value!!.step).isEqualTo(0)
        assertThat(vm.draft.value!!.pars).hasSize(9)
    }

    @Test fun addHole_and_deleteHole() {
        val vm = vm()
        vm.addHole()
        assertThat(vm.draft.value!!.pars).hasSize(10)
        vm.deleteHole(0)
        assertThat(vm.draft.value!!.pars).hasSize(9)
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

    @Test fun loadForEdit_populatesDraftWithPars() {
        val vm = CourseWizardViewModel()
        vm.loadForEdit(venueId = 5, courseId = 8, venueName = "한강", courseName = "B코스", pars = listOf(3, 4, 5))
        val d = vm.draft.value!!
        assertThat(d.editingVenueId).isEqualTo(5)
        assertThat(d.editingCourseId).isEqualTo(8)
        assertThat(d.pars).containsExactly(3, 4, 5).inOrder()
    }

    @Test fun isEditing_reflectsEditingCourseId() {
        assertThat(vm().isEditing()).isFalse()
        val vm = CourseWizardViewModel()
        vm.loadForEdit(1, 2, "v", "c", listOf(3))
        assertThat(vm.isEditing()).isTrue()
    }
}
