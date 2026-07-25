package com.parkgolf.score.data

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.data.db.Converters
import com.parkgolf.score.domain.model.HoleSpec
import org.junit.Test

class ConvertersTest {
    private val c = Converters()

    @Test fun intList_roundTrips() {
        val list = listOf(3, 3, 4, 5)
        assertThat(c.toIntList(c.fromIntList(list))).isEqualTo(list)
    }

    @Test fun stringList_roundTrips() {
        val list = listOf("나", "김철수")
        assertThat(c.toStringList(c.fromStringList(list))).isEqualTo(list)
    }

    @Test fun holeList_roundTrips() {
        val list = listOf(HoleSpec("A", 1, 3), HoleSpec("B", 2, 4))
        assertThat(c.toHoleList(c.fromHoleList(list))).isEqualTo(list)
    }

    @Test fun scoreMatrix_roundTrips_withNulls() {
        val m = listOf(listOf(3, null, 5), listOf(null, 4, 4))
        assertThat(c.toScoreMatrix(c.fromScoreMatrix(m))).isEqualTo(m)
    }
}
