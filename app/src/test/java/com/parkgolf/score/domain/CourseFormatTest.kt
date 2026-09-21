package com.parkgolf.score.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CourseFormatTest {
    @Test fun holesPar_sums_pars_and_counts_holes() {
        assertEquals("9홀 · 파27", CourseFormat.holesPar(List(9) { 3 }))
    }
    @Test fun holesPar_mixed_pars() {
        assertEquals("3홀 · 파10", CourseFormat.holesPar(listOf(3, 4, 3)))
    }
    @Test fun holesPar_empty() {
        assertEquals("0홀 · 파0", CourseFormat.holesPar(emptyList()))
    }
}
