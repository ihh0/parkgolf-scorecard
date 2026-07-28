package com.parkgolf.score.ui.courses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CourseWizardViewModel : ViewModel() {
    private val _draft = MutableLiveData(CourseDraft())
    val draft: LiveData<CourseDraft> get() = _draft

    private fun cur() = _draft.value!!
    private fun update(block: (CourseDraft) -> CourseDraft) { _draft.value = block(cur()) }

    fun initNew(suggestedCourseName: String) {
        _draft.value = CourseDraft(courseName = suggestedCourseName)
    }

    fun loadForEdit(venueId: Long, courseId: Long, venueName: String, courseName: String, pars: List<Int>) {
        _draft.value = CourseDraft(
            editingVenueId = venueId,
            editingCourseId = courseId,
            venueName = venueName,
            courseName = courseName,
            holeCount = pars.size,
            pars = pars,
        )
    }

    fun setVenueName(v: String) = update { it.copy(venueName = v) }
    fun setCourseName(v: String) = update { it.copy(courseName = v) }

    fun setHoleCount(n: Int) = update {
        val clamped = n.coerceIn(1, CourseWizardLogic.MAX_HOLES)
        it.copy(holeCount = clamped, pars = CourseWizardLogic.resizePars(it.pars, clamped))
    }

    fun setPar(index: Int, value: Int) = update {
        val pars = it.pars.toMutableList()
        if (index in pars.indices) pars[index] = value.coerceAtLeast(CourseWizardLogic.MIN_PAR)
        it.copy(pars = pars)
    }

    fun next() = update { it.copy(step = (it.step + 1).coerceAtMost(CourseWizardLogic.MAX_STEP)) }
    fun prev() = update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    fun goToStep(step: Int) = update { it.copy(step = step.coerceIn(0, CourseWizardLogic.MAX_STEP)) }

    fun canAdvance(): Boolean = CourseWizardLogic.isStepValid(cur(), cur().step)
    fun isEditing(): Boolean = cur().editingCourseId != null
}
