package com.parkgolf.score.domain

import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.ParRelation

object Scoring {
    fun total(scores: List<Int?>): Int = scores.filterNotNull().sum()

    /** 플레이어의 0..uptoHoleIndex(포함) 누적 타수. null 점수는 제외. */
    fun runningStrokes(scores: List<Int?>, uptoHoleIndex: Int): Int =
        scores.take((uptoHoleIndex + 1).coerceAtLeast(0)).filterNotNull().sum()

    fun parTotal(holes: List<HoleSpec>): Int = holes.sumOf { it.par }

    fun relativeToPar(scores: List<Int?>, holes: List<HoleSpec>): Int =
        scores.indices
            .filter { scores[it] != null }
            .sumOf { scores[it]!! - holes[it].par }

    fun relationLabel(diff: Int): String = when {
        diff < 0 -> diff.toString()
        diff == 0 -> "E"
        else -> "+$diff"
    }

    fun relation(diff: Int): ParRelation = when {
        diff < 0 -> ParRelation.UNDER
        diff == 0 -> ParRelation.EVEN
        else -> ParRelation.OVER
    }
}
