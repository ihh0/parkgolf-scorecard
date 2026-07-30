package com.parkgolf.score.ui.courses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CourseWizardViewModel : ViewModel() {
    private val _draft = MutableLiveData(CourseDraft())
    val draft: LiveData<CourseDraft> get() = _draft

    private fun cur() = _draft.value!!
    private fun update(block: (CourseDraft) -> CourseDraft) { _draft.value = block(cur()) }

    fun initNew(suggestedCourseName: String) { _draft.value = CourseDraft(courseName = suggestedCourseName) }

    fun loadForEdit(venueId: Long, courseId: Long, venueName: String, courseName: String, pars: List<Int>) {
        _draft.value = CourseDraft(
            editingVenueId = venueId, editingCourseId = courseId,
            venueName = venueName, courseName = courseName, pars = pars,
        )
    }

    fun setVenueName(v: String) = update { it.copy(venueName = v) }
    fun setCourseName(v: String) = update { it.copy(courseName = v) }

    fun addHole() = update { it.copy(pars = CourseWizardLogic.addHole(it.pars)) }
    fun deleteHole(index: Int) = update { it.copy(pars = CourseWizardLogic.deleteHole(it.pars, index)) }
    fun setPar(index: Int, value: Int) = update {
        val pars = it.pars.toMutableList()
        if (index in pars.indices) pars[index] = CourseWizardLogic.clampPar(value)
        it.copy(pars = pars)
    }

    fun next() = update { it.copy(step = (it.step + 1).coerceAtMost(CourseWizardLogic.MAX_STEP)) }
    fun prev() = update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    fun goToStep(step: Int) = update { it.copy(step = step.coerceIn(0, CourseWizardLogic.MAX_STEP)) }

    fun isEditing(): Boolean = cur().editingCourseId != null
}
