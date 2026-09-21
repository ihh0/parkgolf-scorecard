package com.parkgolf.score.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.parkgolf.score.domain.ScoreEditor
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.Round

/** Activity-scoped holder for the round currently being played/edited. */
class RoundSessionViewModel : ViewModel() {
    val round = MutableLiveData<Round?>(null)

    /**
     * 진행 중 라운드 이어하기 팝업을 이번 앱 실행에서 이미 물어봤는지.
     * 홈으로 되돌아올 때마다 반복 노출되는 것을 막는다. 프로세스가 죽고
     * 재실행되면 ViewModel이 새로 생성돼 false로 초기화된다.
     */
    var resumePrompted = false

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
