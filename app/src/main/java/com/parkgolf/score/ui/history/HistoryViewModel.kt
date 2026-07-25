package com.parkgolf.score.ui.history

import androidx.lifecycle.ViewModel
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.Stats
import com.parkgolf.score.domain.model.Round

data class HistoryStats(val recentAverage: Double?, val best: Int?, val worst: Int?)

class HistoryViewModel : ViewModel() {
    companion object {
        /** Stats use the primary player's (index 0, "나") total per round. */
        fun computeStats(rounds: List<Round>, recentN: Int): HistoryStats {
            val totals = rounds.sortedByDescending { it.date }
                .map { Scoring.total(it.scores[0]) }
            return HistoryStats(
                recentAverage = Stats.recentAverageStrokes(totals, recentN),
                best = Stats.best(totals),
                worst = Stats.worst(totals)
            )
        }
    }
}
