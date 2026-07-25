package com.parkgolf.score.domain

import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.ParRelation

object Scoring {
    fun total(scores: List<Int?>): Int = scores.filterNotNull().sum()

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
