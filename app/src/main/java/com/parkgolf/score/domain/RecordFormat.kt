package com.parkgolf.score.domain

import com.parkgolf.score.domain.model.Round

/** 기록 목록 카드용 순수 포맷 헬퍼. 색상은 뷰 계층에서 결정. */
object RecordFormat {
    /** 파 대비 차이를 배지 텍스트로. 언더=음수 그대로, 이븐="E", 오버="+n". */
    fun badgeText(diff: Int): String = when {
        diff < 0 -> diff.toString()
        diff == 0 -> "E"
        else -> "+$diff"
    }

    /** 라운드 홀들의 코스명 중 첫 비어있지 않은 값. 없으면 "". */
    fun courseName(round: Round): String =
        round.holes.map { it.courseName }.firstOrNull { it.isNotBlank() } ?: ""
}
