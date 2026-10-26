package com.parkgolf.score.ui.settings

/** 앱 내 글꼴 크기 프리셋. multiplier는 Configuration.fontScale에 절대값으로 적용된다. */
enum class FontScaleKey(val stored: String, val multiplier: Float) {
    NORMAL("normal", 1.0f),
    LARGE("large", 1.15f),
    XLARGE("xlarge", 1.3f),
    XXLARGE("xxlarge", 1.5f),
    XXXLARGE("xxxlarge", 1.7f),
    XXXXLARGE("xxxxlarge", 1.9f);

    companion object {
        fun fromStored(s: String?): FontScaleKey = entries.firstOrNull { it.stored == s } ?: NORMAL
    }
}
