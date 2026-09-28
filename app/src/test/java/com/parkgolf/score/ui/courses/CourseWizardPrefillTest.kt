package com.parkgolf.score.ui.courses

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CourseWizardPrefillTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    @Test fun initPrefill_sets_name_course_and_pars() {
        val vm = CourseWizardViewModel()
        vm.initPrefill("양재천 파크골프장", "동코스", 18)
        val d = vm.draft.value!!
        assertEquals("양재천 파크골프장", d.venueName)
        assertEquals("동코스", d.courseName)
        assertEquals(18, d.pars.size)
        assertEquals(List(18) { 3 }, d.pars)
        assertEquals(0, d.step)
    }
    @Test fun initPrefill_min_one_hole() {
        val vm = CourseWizardViewModel()
        vm.initPrefill("V", "C", 0)
        assertEquals(1, vm.draft.value!!.pars.size)
    }
}
