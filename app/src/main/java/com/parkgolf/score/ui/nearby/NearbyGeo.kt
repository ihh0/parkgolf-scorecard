package com.parkgolf.score.ui.nearby

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object NearbyGeo {
    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1); val dL = Math.toRadians(lng2 - lng1)
        val a = sin(dPhi / 2) * sin(dPhi / 2) + cos(p1) * cos(p2) * sin(dL / 2) * sin(dL / 2)
        return 2 * r * asin(sqrt(a))
    }

    /** 좌표 있는 구장만, 거리 오름차순 상위 limit개. */
    fun nearest(venues: List<NearbyVenue>, myLat: Double, myLng: Double, limit: Int = 10):
        List<Pair<NearbyVenue, Double>> =
        venues.filter { it.lat != null && it.lng != null }
            .map { it to haversineKm(myLat, myLng, it.lat!!, it.lng!!) }
            .sortedBy { it.second }
            .take(limit)

    /** 1km 미만은 m(10 단위 반올림), 이상은 km(소수1). */
    fun formatDistance(km: Double, mFmt: String, kmFmt: String): String =
        if (km < 1.0) String.format(mFmt, (Math.round(km * 1000 / 10.0) * 10).toInt())
        else String.format(kmFmt, km)
}
