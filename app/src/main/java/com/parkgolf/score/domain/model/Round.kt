package com.parkgolf.score.domain.model

enum class ParRelation { UNDER, EVEN, OVER }

enum class RoundStatus { IN_PROGRESS, COMPLETED }

/**
 * A round snapshots everything it needs: player names, the ordered holes (with pars),
 * and a scores matrix indexed [playerIndex][holeIndex]. A null score means "not entered yet".
 */
data class Round(
    val id: Long = 0,
    val date: Long,                       // epoch millis
    val venueName: String,
    val players: List<String>,            // size 1..4
    val holes: List<HoleSpec>,
    val scores: List<List<Int?>>,         // [player][hole]
    val status: RoundStatus
)
