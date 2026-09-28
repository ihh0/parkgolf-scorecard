package com.parkgolf.score.ui.nearby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NearbyParseTest {
    private val json = """
      [{"name":"거창스포츠파크 파크골프장","region":{"sido":"경상남도","sigungu":"거창군"},
        "roadAddress":"경남 거창읍 심소정길 39","jibunAddress":"경남 거창읍 양평리 1160",
        "lat":35.6951,"lng":127.9263,"coordSource":"original",
        "courses":[{"name":"1구장","holes":18},{"name":"2구장","holes":18}],
        "phone":"055-940-8720","operator":"거창군","source":"x.csv"},
       {"name":"좌표없음","region":{"sido":null,"sigungu":"어딘가"},"roadAddress":null,
        "jibunAddress":"주소","lat":null,"lng":null,"coordSource":"none",
        "courses":[{"name":"","holes":null}],"phone":null,"operator":null,"source":"y.csv"}]
    """.trimIndent()

    @Test fun parses_fields_and_nulls() {
        val vs = NearbyRepository.parseVenues(json)
        assertEquals(2, vs.size)
        assertEquals("거창스포츠파크 파크골프장", vs[0].name)
        assertEquals(35.6951, vs[0].lat!!, 1e-6)
        assertEquals(listOf(18, 18), vs[0].courses.map { it.holes })
        assertEquals("거창군", vs[0].sigungu)
        assertNull(vs[1].lat)
        assertNull(vs[1].courses[0].holes)
    }
}
