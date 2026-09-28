package com.parkgolf.score.ui.nearby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyGeoTest {
    private fun v(name: String, lat: Double?, lng: Double?) =
        NearbyVenue(name, lat, lng, listOf(NearbyCourse("A", 9)), null, null, null, null, null)

    @Test fun haversine_zero() {
        assertEquals(0.0, NearbyGeo.haversineKm(35.0, 127.0, 35.0, 127.0), 1e-6)
    }
    @Test fun haversine_seoul_busan() {
        val d = NearbyGeo.haversineKm(37.5665, 126.9780, 35.1796, 129.0756)
        assertTrue("$d", d in 300.0..340.0)
    }
    @Test fun nearest_sorts_and_limits_and_skips_null() {
        val vs = listOf(
            v("far", 35.9, 127.9), v("near", 37.57, 126.98),
            v("mid", 36.5, 127.5), v("nocoord", null, null),
        )
        val r = NearbyGeo.nearest(vs, 37.5665, 126.9780, limit = 2)
        assertEquals(listOf("near", "mid"), r.map { it.first.name })
        assertTrue(r[0].second < r[1].second)
    }
    @Test fun format_distance_m_and_km() {
        assertEquals("850m", NearbyGeo.formatDistance(0.85, mFmt = "%1\$dm", kmFmt = "%1$.1fkm"))
        assertEquals("1.2km", NearbyGeo.formatDistance(1.23, mFmt = "%1\$dm", kmFmt = "%1$.1fkm"))
    }
}
