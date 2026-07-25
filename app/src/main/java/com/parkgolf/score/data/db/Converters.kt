package com.parkgolf.score.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.parkgolf.score.domain.model.HoleSpec

class Converters {
    private val gson = Gson()

    @TypeConverter fun fromIntList(v: List<Int>): String = gson.toJson(v)
    @TypeConverter fun toIntList(v: String): List<Int> =
        gson.fromJson(v, object : TypeToken<List<Int>>() {}.type)

    @TypeConverter fun fromStringList(v: List<String>): String = gson.toJson(v)
    @TypeConverter fun toStringList(v: String): List<String> =
        gson.fromJson(v, object : TypeToken<List<String>>() {}.type)

    @TypeConverter fun fromHoleList(v: List<HoleSpec>): String = gson.toJson(v)
    @TypeConverter fun toHoleList(v: String): List<HoleSpec> =
        gson.fromJson(v, object : TypeToken<List<HoleSpec>>() {}.type)

    @TypeConverter fun fromScoreMatrix(v: List<List<Int?>>): String = gson.toJson(v)
    @TypeConverter fun toScoreMatrix(v: String): List<List<Int?>> =
        gson.fromJson(v, object : TypeToken<List<List<Int?>>>() {}.type)
}
