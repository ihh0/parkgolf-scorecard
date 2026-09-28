package com.parkgolf.score.ui.nearby

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object NearbyRepository {
    @Volatile private var cache: List<NearbyVenue>? = null

    fun parseVenues(json: String): List<NearbyVenue> {
        val arr = JSONArray(json)
        val out = ArrayList<NearbyVenue>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val region = o.optJSONObject("region")
            val cArr = o.optJSONArray("courses") ?: JSONArray()
            val courses = (0 until cArr.length()).map { j ->
                val c = cArr.getJSONObject(j)
                NearbyCourse(c.optString("name", ""), c.optIntOrNull("holes"))
            }
            out.add(
                NearbyVenue(
                    name = o.optString("name", ""),
                    lat = o.optDoubleOrNull("lat"),
                    lng = o.optDoubleOrNull("lng"),
                    courses = courses,
                    sido = region?.optStringOrNull("sido"),
                    sigungu = region?.optStringOrNull("sigungu"),
                    roadAddress = o.optStringOrNull("roadAddress"),
                    jibunAddress = o.optStringOrNull("jibunAddress"),
                    phone = o.optStringOrNull("phone"),
                )
            )
        }
        return out
    }

    suspend fun load(context: Context): List<NearbyVenue> {
        cache?.let { return it }
        val json = context.assets.open("parkgolf_venues.json")
            .bufferedReader().use { it.readText() }
        return parseVenues(json).also { cache = it }
    }
}

private fun JSONObject.optDoubleOrNull(k: String): Double? =
    if (isNull(k) || !has(k)) null else optDouble(k).takeIf { !it.isNaN() }
private fun JSONObject.optIntOrNull(k: String): Int? =
    if (isNull(k) || !has(k)) null else optInt(k)
private fun JSONObject.optStringOrNull(k: String): String? =
    if (isNull(k) || !has(k)) null else optString(k).takeIf { it.isNotEmpty() }
