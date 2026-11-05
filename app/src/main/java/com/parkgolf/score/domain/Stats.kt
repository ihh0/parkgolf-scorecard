package com.parkgolf.score.domain

import com.parkgolf.score.domain.model.Round
import java.time.Instant
import java.time.ZoneId

/** 한 게임의 파 대비 성적(주 플레이어 index 0 기준). */
data class GameRelativeScore(
    val date: Long,
    val courseLabel: String,
    val relativeToPar: Int,
)

enum class StatsPeriod { RECENT_10, THREE_MONTHS, ALL }

/** 파 대비 요약. 비어 있으면 count=0, 나머지 null. */
data class RelativeSummary(val count: Int, val average: Double?, val best: Int?, val worst: Int?)

object Stats {
    /** Average of the most recent [n] totals (list is newest-first). Null if empty. */
    fun recentAverageStrokes(totals: List<Int>, n: Int): Double? {
        if (totals.isEmpty()) return null
        val window = totals.take(n)
        return window.sum().toDouble() / window.size
    }

    fun best(totals: List<Int>): Int? = totals.minOrNull()
    fun worst(totals: List<Int>): Int? = totals.maxOrNull()

    /** 완료 라운드 → 주 플레이어 파 대비 성적. 날짜 오름차순 반환. */
    fun relativeScores(rounds: List<Round>): List<GameRelativeScore> =
        rounds.map {
            GameRelativeScore(
                date = it.date,
                courseLabel = RecordFormat.courseLabel(it),
                relativeToPar = Scoring.relativeToPar(it.scores[0], it.holes),
            )
        }.sortedBy { it.date }

    /** label == null 이면 전체, 아니면 courseLabel 일치만. */
    fun filterByCourse(scores: List<GameRelativeScore>, label: String?): List<GameRelativeScore> =
        if (label == null) scores else scores.filter { it.courseLabel == label }

    /** 기간 필터(입력은 날짜 오름차순 가정). */
    fun filterByPeriod(
        scores: List<GameRelativeScore>,
        period: StatsPeriod,
        nowMillis: Long,
    ): List<GameRelativeScore> = when (period) {
        StatsPeriod.RECENT_10 -> scores.takeLast(10)
        StatsPeriod.ALL -> scores
        StatsPeriod.THREE_MONTHS -> {
            val threshold = Instant.ofEpochMilli(nowMillis)
                .atZone(ZoneId.systemDefault())
                .minusMonths(3)
                .toInstant().toEpochMilli()
            scores.filter { it.date >= threshold }
        }
    }

    fun summarize(scores: List<GameRelativeScore>): RelativeSummary {
        if (scores.isEmpty()) return RelativeSummary(0, null, null, null)
        val rels = scores.map { it.relativeToPar }
        return RelativeSummary(
            count = rels.size,
            average = rels.average(),
            best = rels.min(),
            worst = rels.max(),
        )
    }

    /** 등장 순서 보존 distinct 코스 라벨. */
    fun distinctCourseLabels(scores: List<GameRelativeScore>): List<String> =
        scores.map { it.courseLabel }.distinct()
}
