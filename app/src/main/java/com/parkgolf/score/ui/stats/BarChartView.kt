package com.parkgolf.score.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.R
import com.parkgolf.score.domain.GameRelativeScore
import com.parkgolf.score.domain.RecordFormat

/** 파 대비 타수 막대그래프. E 기준선 위=오버파, 아래=언더파. */
class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private var data: List<GameRelativeScore> = emptyList()
    private val d = resources.displayMetrics.density
    private val slotW = 56 * d
    private val barW = 30 * d

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = d }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = 13 * d
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = 12 * d
    }

    private val underColor = MaterialColors.getColor(this, R.attr.parkPrimary, Color.GREEN)
    private val overColor = MaterialColors.getColor(this, R.attr.parkSecondary, Color.RED)
    private val mutedColor = MaterialColors.getColor(this, R.attr.parkMutedForeground, Color.GRAY)
    private val fgColor = MaterialColors.getColor(this, R.attr.parkForeground, Color.BLACK)
    private val borderColor = MaterialColors.getColor(this, R.attr.parkBorder, Color.LTGRAY)

    fun setData(scores: List<GameRelativeScore>) {
        data = scores; requestLayout(); invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = resolveSize((240 * d).toInt(), heightMeasureSpec)
        val contentW = (data.size * slotW).toInt()
        val w = maxOf(resolveSize(contentW, widthMeasureSpec), contentW)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) return
        val top = 20 * d           // 값 라벨 여유
        val bottom = height - 20 * d // x축 라벨 여유
        val plotH = bottom - top
        val maxOver = maxOf(1, data.maxOf { maxOf(it.relativeToPar, 0) })
        val maxUnder = maxOf(1, data.maxOf { maxOf(-it.relativeToPar, 0) })
        val range = (maxOver + maxUnder).toFloat()
        val baselineY = top + plotH * (maxOver / range)

        // E 기준선
        linePaint.color = borderColor
        canvas.drawLine(0f, baselineY, width.toFloat(), baselineY, linePaint)
        labelPaint.color = mutedColor
        labelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("E", 2 * d, baselineY - 3 * d, labelPaint) // 골프 공용 기호(E=이븐)
        labelPaint.textAlign = Paint.Align.CENTER

        data.forEachIndexed { i, g ->
            val cx = i * slotW + slotW / 2
            val rel = g.relativeToPar
            val stub = 3 * d
            if (rel >= 0) {
                val barH = if (rel == 0) stub else plotH * (rel / range)
                barPaint.color = if (rel == 0) mutedColor else overColor
                canvas.drawRoundRect(cx - barW / 2, baselineY - barH, cx + barW / 2, baselineY, 4 * d, 4 * d, barPaint)
                labelPaint.color = fgColor
                canvas.drawText(RecordFormat.badgeText(rel), cx, baselineY - barH - 5 * d, labelPaint)
            } else {
                val barH = plotH * (-rel / range)
                barPaint.color = underColor
                canvas.drawRoundRect(cx - barW / 2, baselineY, cx + barW / 2, baselineY + barH, 4 * d, 4 * d, barPaint)
                labelPaint.color = fgColor
                canvas.drawText(RecordFormat.badgeText(rel), cx, baselineY + barH + 14 * d, labelPaint)
            }
            axisPaint.color = mutedColor
            canvas.drawText((i + 1).toString(), cx, height - 5 * d, axisPaint)
        }
    }
}
