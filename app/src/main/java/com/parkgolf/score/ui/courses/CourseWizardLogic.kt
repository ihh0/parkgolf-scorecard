package com.parkgolf.score.ui.courses

import com.parkgolf.score.data.db.entity.VenueEntity

/** All wizard state in one immutable value. step is 0..3 (4 = success handled by fragment). */
data class CourseDraft(
    val editingVenueId: Long? = null,
    val editingCourseId: Long? = null,
    val venueName: String = "",
    val courseName: String = "",
    val pars: List<Int> = List(9) { 3 },
    val step: Int = 0,
)

/** Pure decision logic for the course wizard; no Android dependencies. */
object CourseWizardLogic {
    const val MIN_PAR = 1
    const val MAX_STEP = 3

    fun addHole(pars: List<Int>): List<Int> = pars + 3

    fun deleteHole(pars: List<Int>, index: Int): List<Int> =
        if (pars.size <= 1 || index !in pars.indices) pars
        else pars.toMutableList().apply { removeAt(index) }

    /** Par floored at 1, no upper bound. */
    fun clampPar(value: Int): Int = value.coerceAtLeast(MIN_PAR)

    /** Exact venue-name match (trim, case-insensitive). */
    fun isExistingVenue(name: String, venues: List<VenueEntity>): Boolean {
        val key = name.trim()
        return key.isNotEmpty() && venues.any { it.name.trim().equals(key, ignoreCase = true) }
    }

    /** Partial matches (venue name contains input), excluding exact matches. name must be non-blank. */
    fun matchedVenues(name: String, venues: List<VenueEntity>): List<String> {
        val key = name.trim()
        if (key.isEmpty()) return emptyList()
        return venues.map { it.name }
            .filter { it.contains(key, ignoreCase = true) && !it.trim().equals(key, ignoreCase = true) }
            .distinct()
    }

    fun isDuplicateCourseName(name: String, existingCourseNames: List<String>): Boolean {
        val key = name.trim()
        return existingCourseNames.any { it.trim().equals(key, ignoreCase = true) }
    }

    fun venueStepValid(draft: CourseDraft): Boolean = draft.venueName.isNotBlank()

    fun courseStepValid(name: String, existingCourseNames: List<String>): Boolean =
        name.isNotBlank() && !isDuplicateCourseName(name, existingCourseNames)

    fun holeStepValid(pars: List<Int>): Boolean = pars.isNotEmpty()

    /** Existing venue id matching [name], or [editingVenueId], else null. */
    fun resolveVenueId(name: String, existing: List<VenueEntity>, editingVenueId: Long?): Long? {
        if (editingVenueId != null) return editingVenueId
        val key = name.trim()
        return existing.firstOrNull { it.name.trim().equals(key, ignoreCase = true) }?.id
    }
}
