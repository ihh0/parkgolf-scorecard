package com.parkgolf.score.ui.nearby

data class NearbyCourse(val name: String, val holes: Int?)

data class NearbyVenue(
    val name: String,
    val lat: Double?,
    val lng: Double?,
    val courses: List<NearbyCourse>,
    val sido: String?,
    val sigungu: String?,
    val roadAddress: String?,
    val jibunAddress: String?,
    val phone: String?,
)
