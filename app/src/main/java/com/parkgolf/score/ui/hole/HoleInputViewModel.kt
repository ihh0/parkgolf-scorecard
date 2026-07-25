package com.parkgolf.score.ui.hole

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HoleInputViewModel(private val totalHoles: Int) : ViewModel() {
    val holeIndex = MutableLiveData(0)

    fun next() { holeIndex.value = (holeIndex.value!! + 1).coerceAtMost(totalHoles - 1) }
    fun prev() { holeIndex.value = (holeIndex.value!! - 1).coerceAtLeast(0) }
    fun goTo(index: Int) { holeIndex.value = index.coerceIn(0, totalHoles - 1) }
    fun isLastHole(): Boolean = holeIndex.value == totalHoles - 1
}
