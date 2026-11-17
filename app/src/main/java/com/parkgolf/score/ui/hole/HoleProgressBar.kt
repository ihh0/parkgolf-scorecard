package com.parkgolf.score.ui.hole

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.R

/** 직선 위 원형 점으로 홀 진행을 표시하는 바. 진행분은 테마색으로 채워진다. 표시 전용. */
class HoleProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var total = 0
    private var current = 0

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    private val track = dp(3f)
    private val rFuture = dp(3.5f)
    private val rDone = dp(4.5f)
    private val rCurrent = dp(7f)
    private val ring = dp(2.5f)
    private val padX = rCurrent + ring + dp(1f)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val colorPrimary get() = MaterialColors.getColor(this, R.attr.parkPrimary)
    // parkBorder는 반투명이라 트랙선과 점이 겹치면 알파가 중첩돼 진해진다.
    // 바가 올라간 카드색 위에 합성해 같은 톤의 불투명 연한 색으로 쓴다.
    private val colorMuted get() = ColorUtils.compositeColors(
        MaterialColors.getColor(this, R.attr.parkBorder),
        ContextCompat.getColor(context, R.color.card)
    )
    private val colorRing get() = MaterialColors.getColor(this, R.attr.parkBackground)

    fun setProgress(total: Int, currentIndex: Int) {
        this.total = total
        this.current = currentIndex.coerceIn(0, (total - 1).coerceAtLeast(0))
        contentDescription = context.getString(R.string.hole_counter, this.current + 1, total)
        invalidate()
    }

    private fun x(i: Int): Float {
        if (total <= 1) return width / 2f
        return padX + i * (width - 2 * padX) / (total - 1)
    }

    override fun onDraw(canvas: Canvas) {
        if (total <= 0) return
        val cy = height / 2f

        if (total >= 2) {
            // 트랙(미진행)
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = track
            paint.color = colorMuted
            canvas.drawLine(x(0), cy, x(total - 1), cy, paint)
            // 채움(진행)
            if (current > 0) {
                paint.color = colorPrimary
                canvas.drawLine(x(0), cy, x(current), cy, paint)
            }
        }

        // 점
        paint.style = Paint.Style.FILL
        for (i in 0 until total) {
            val cx = x(i)
            when {
                i < current -> { paint.color = colorPrimary; canvas.drawCircle(cx, cy, rDone, paint) }
                i > current -> { paint.color = colorMuted; canvas.drawCircle(cx, cy, rFuture, paint) }
                else -> {
                    // 현재: 하이라이트 링 + 큰 parkPrimary 원
                    paint.color = colorRing
                    canvas.drawCircle(cx, cy, rCurrent + ring, paint)
                    paint.color = colorPrimary
                    canvas.drawCircle(cx, cy, rCurrent, paint)
                }
            }
        }
    }
}
