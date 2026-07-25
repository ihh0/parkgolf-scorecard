package com.parkgolf.score.ui.start

import androidx.lifecycle.ViewModel
import com.parkgolf.score.domain.model.Round

class StartViewModel : ViewModel() {
    companion object {
        /** Most-recent completed round per venue, newest first, up to [limit]. */
        fun deriveRecentCourses(completed: List<Round>, limit: Int): List<RecentCourse> =
            completed.sortedByDescending { it.date }
                .distinctBy { it.venueName }
                .take(limit)
                .map { RecentCourse(it.venueName, it.holes.size, it.date) }
    }
}
