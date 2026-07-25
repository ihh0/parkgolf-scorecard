package com.parkgolf.score.data

import com.parkgolf.score.data.db.entity.RoundEntity
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus

fun RoundEntity.toDomain(): Round = Round(
    id = id,
    date = date,
    venueName = venueName,
    players = players,
    holes = holes,
    scores = scores,
    status = RoundStatus.valueOf(status)
)

fun Round.toEntity(): RoundEntity = RoundEntity(
    id = id,
    date = date,
    venueName = venueName,
    players = players,
    holes = holes,
    scores = scores,
    status = status.name
)
