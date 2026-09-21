package com.parkgolf.score.domain

/** 코스 목록용 순수 포맷 헬퍼. */
object CourseFormat {
    /** "N홀 · 파X" (X = 파 합계). */
    fun holesPar(pars: List<Int>): String = "${pars.size}홀 · 파${pars.sum()}"
}
