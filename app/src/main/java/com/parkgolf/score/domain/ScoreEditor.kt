package com.parkgolf.score.domain

import com.parkgolf.score.domain.model.Round

object ScoreEditor {
    private const val MIN_STROKES = 1

    fun set(round: Round, playerIndex: Int, holeIndex: Int, value: Int): Round {
        val newScores = round.scores.map { it.toMutableList() }
        newScores[playerIndex][holeIndex] = value.coerceAtLeast(MIN_STROKES)
        return round.copy(scores = newScores)
    }

    fun adjust(round: Round, playerIndex: Int, holeIndex: Int, delta: Int): Round {
        val current = round.scores[playerIndex][holeIndex] ?: 0
        return set(round, playerIndex, holeIndex, current + delta)
    }
}
