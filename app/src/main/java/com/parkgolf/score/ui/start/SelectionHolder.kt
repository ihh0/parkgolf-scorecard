package com.parkgolf.score.ui.start

/** Carries the in-flight round selection between the start setup fragments. */
object SelectionHolder {
    var venueId: Long? = null
    var venueName: String? = null
    val chosenCourseIds: MutableList<Long> = mutableListOf() // ordered courses to play
    fun reset() {
        venueId = null; venueName = null; chosenCourseIds.clear()
    }
}
