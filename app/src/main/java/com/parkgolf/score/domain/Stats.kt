package com.parkgolf.score.domain

object Stats {
    /** Average of the most recent [n] totals (list is newest-first). Null if empty. */
    fun recentAverageStrokes(totals: List<Int>, n: Int): Double? {
        if (totals.isEmpty()) return null
        val window = totals.take(n)
        return window.sum().toDouble() / window.size
    }

    fun best(totals: List<Int>): Int? = totals.minOrNull()
    fun worst(totals: List<Int>): Int? = totals.maxOrNull()
}
