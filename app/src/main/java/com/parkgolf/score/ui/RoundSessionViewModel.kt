package com.parkgolf.score.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.parkgolf.score.domain.ScoreEditor
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.Round

/** Activity-scoped holder for the round currently being played/edited. */
class RoundSessionViewModel : ViewModel() {
    val round = MutableLiveData<Round?>(null)

    fun startRound(newRound: Round) { round.value = newRound }

    fun adjust(playerIndex: Int, holeIndex: Int, delta: Int) {
        val r = round.value ?: return
        round.value = ScoreEditor.adjust(r, playerIndex, holeIndex, delta)
    }

    fun cumulativeRelative(playerIndex: Int): Int {
        val r = round.value ?: return 0
        return Scoring.relativeToPar(r.scores[playerIndex], r.holes)
    }
}
