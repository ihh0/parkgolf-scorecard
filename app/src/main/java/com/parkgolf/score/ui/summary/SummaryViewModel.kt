package com.parkgolf.score.ui.summary

import androidx.lifecycle.ViewModel
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.Round

data class RankRow(val rank: Int, val player: String, val total: Int, val relative: Int)

class SummaryViewModel : ViewModel() {
    companion object {
        fun ranking(round: Round): List<RankRow> {
            val base = round.players.indices.map { p ->
                Triple(
                    round.players[p],
                    Scoring.total(round.scores[p]),
                    Scoring.relativeToPar(round.scores[p], round.holes)
                )
            }.sortedBy { it.second }

            var lastTotal = Int.MIN_VALUE
            var lastRank = 0
            return base.mapIndexed { i, (name, total, rel) ->
                val rank = if (total == lastTotal) lastRank else (i + 1)
                lastRank = rank
                lastTotal = total
                RankRow(rank, name, total, rel)
            }
        }
    }
}
