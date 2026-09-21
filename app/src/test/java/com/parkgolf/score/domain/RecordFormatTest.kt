package com.parkgolf.score.domain

import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordFormatTest {
    private fun round(courseNames: List<String>): Round {
        val holes = courseNames.mapIndexed { i, c -> HoleSpec(c, i + 1, 3) }
        return Round(
            id = 1, date = 0L, venueName = "V",
            players = listOf("나"), holes = holes,
            scores = listOf(List(holes.size) { 3 }), status = RoundStatus.COMPLETED
        )
    }

    @Test fun badge_under_shows_signed_negative() {
        assertEquals("-3", RecordFormat.badgeText(-3))
    }

    @Test fun badge_even_shows_E() {
        assertEquals("E", RecordFormat.badgeText(0))
    }

    @Test fun badge_over_shows_plus() {
        assertEquals("+2", RecordFormat.badgeText(2))
    }

    @Test fun courseName_takes_first_non_blank() {
        assertEquals("A코스", RecordFormat.courseName(round(listOf("", "A코스", "A코스"))))
    }

    @Test fun courseName_blank_when_none() {
        assertEquals("", RecordFormat.courseName(round(listOf("", ""))))
    }
}
