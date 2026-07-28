package com.parkgolf.score.ui.courses

import com.parkgolf.score.data.db.entity.VenueEntity

/** All wizard state in one immutable value. step is 0..3. */
data class CourseDraft(
    val editingVenueId: Long? = null,
    val editingCourseId: Long? = null,
    val venueName: String = "",
    val courseName: String = "A코스",
    val holeCount: Int = 9,
    val pars: List<Int> = List(9) { 3 },
    val step: Int = 0,
)

/** Pure decision logic for the course wizard; no Android dependencies. */
object CourseWizardLogic {
    const val MIN_PAR = 1
    const val MAX_STEP = 3
    const val MAX_HOLES = 27

    /** Resize [current] to [holeCount], padding new holes with par 3. */
    fun resizePars(current: List<Int>, holeCount: Int): List<Int> {
        val n = holeCount.coerceAtLeast(1)
        return when {
            current.size == n -> current
            current.size < n -> current + List(n - current.size) { 3 }
            else -> current.subList(0, n).toList()
        }
    }

    /** Next unused course letter as "X코스", based on the first char of existing names. */
    fun suggestCourseName(existingCourseNames: List<String>): String {
        val used = existingCourseNames.mapNotNull { it.trim().firstOrNull() }.toSet()
        val next = ('A'..'Z').firstOrNull { it !in used } ?: 'A'
        return "${next}코스"
    }

    /** Existing venue id matching [name] (trim, case-insensitive), or [editingVenueId], else null (= create new). */
    fun resolveVenueId(name: String, existing: List<VenueEntity>, editingVenueId: Long?): Long? {
        if (editingVenueId != null) return editingVenueId
        val key = name.trim()
        return existing.firstOrNull { it.name.trim().equals(key, ignoreCase = true) }?.id
    }

    fun isStepValid(draft: CourseDraft, step: Int): Boolean = when (step) {
        0 -> draft.venueName.isNotBlank()
        1 -> draft.courseName.isNotBlank()
        2 -> draft.holeCount in 1..MAX_HOLES &&
            draft.pars.size == draft.holeCount &&
            draft.pars.all { it >= MIN_PAR }
        else -> true
    }
}
