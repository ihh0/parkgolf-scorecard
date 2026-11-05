package com.parkgolf.score.ui.stats

import androidx.lifecycle.ViewModel
import com.parkgolf.score.domain.GameRelativeScore
import com.parkgolf.score.domain.RelativeSummary
import com.parkgolf.score.domain.Stats
import com.parkgolf.score.domain.StatsPeriod
import com.parkgolf.score.domain.model.Round

class StatsViewModel : ViewModel() {
    var period: StatsPeriod = StatsPeriod.RECENT_10
    var courseLabel: String? = null // null = 전체 코스

    data class ViewState(
        val graph: List<GameRelativeScore>,
        val summary: RelativeSummary,
        val courseOptions: List<String>, // "전체 코스" 제외한 distinct 라벨
    )

    fun compute(rounds: List<Round>, nowMillis: Long): ViewState {
        val all = Stats.relativeScores(rounds)
        val options = Stats.distinctCourseLabels(all)
        val byCourse = Stats.filterByCourse(all, courseLabel)
        val graph = Stats.filterByPeriod(byCourse, period, nowMillis)
        return ViewState(graph, Stats.summarize(graph), options)
    }
}
