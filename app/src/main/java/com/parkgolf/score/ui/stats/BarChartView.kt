package com.parkgolf.score.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.R
import com.parkgolf.score.domain.GameRelativeScore
import com.parkgolf.score.domain.RecordFormat

/** 파 대비 타수 차트. E 기준선 위=오버파(빨강), 아래=언더파(파랑). 막대/꺾은선 모드. */
class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private var data: List<GameRelativeScore> = emptyList()
    private var mode: ChartMode = ChartMode.BAR
    private val d = resources.displayMetrics.density
    private val fsd = d * resources.configuration.fontScale
    private val slotW = 56 * fsd
    private val barW = 30 * d
    private val maxChartH = 260 * d

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2 * d; strokeCap = Paint.Cap.ROUND
    }
    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = d }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = 13 * fsd
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = 12 * fsd
    }

    private val overColor = ContextCompat.getColor(context, R.color.chart_over)
    private val underColor = ContextCompat.getColor(context, R.color.chart_under)
    private val mutedColor = MaterialColors.getColor(this, R.attr.parkMutedForeground, Color.GRAY)
    private val fgColor = MaterialColors.getColor(this, R.attr.parkForeground, Color.BLACK)
    private val borderColor = MaterialColors.getColor(this, R.attr.parkBorder, Color.LTGRAY)

    fun setData(scores: List<GameRelativeScore>) { data = scores; requestLayout(); invalidate() }
    fun setMode(m: ChartMode) { if (mode != m) { mode = m; invalidate() } }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = resolveSize((260 * d).toInt(), heightMeasureSpec)
        val contentW = (data.size * slotW).toInt()
        val w = maxOf(resolveSize(contentW, widthMeasureSpec), contentW)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) return
        val topBand = 13 * fsd + 7 * d
        val underLabelBand = 13 * fsd + 5 * d
        val axisBand = 12 * fsd + 10 * d
        val chartH = minOf(height.toFloat(), maxChartH)
        val chartTop = (height - chartH) / 2f

        val plotTop = chartTop + topBand
        val plotBottom = chartTop + chartH - axisBand - underLabelBand
        val plotH = plotBottom - plotTop
        val maxOver = maxOf(1, data.maxOf { maxOf(it.relativeToPar, 0) })
        val maxUnder = maxOf(1, data.maxOf { maxOf(-it.relativeToPar, 0) })
        val range = (maxOver + maxUnder).toFloat()
        val baselineY = plotTop + plotH * (maxOver / range)
        val xAxisY = chartTop + chartH - 5 * d

        baselinePaint.color = borderColor
        canvas.drawLine(0f, baselineY, width.toFloat(), baselineY, baselinePaint)

        fun cx(i: Int) = i * slotW + slotW / 2
        fun colorFor(rel: Int) = when { rel > 0 -> overColor; rel < 0 -> underColor; else -> mutedColor }

        if (mode == ChartMode.LINE) {
            linePaint.color = borderColor
            for (i in 0 until data.size - 1) {
                val y1 = baselineY - plotH * (data[i].relativeToPar / range)
                val y2 = baselineY - plotH * (data[i + 1].relativeToPar / range)
                canvas.drawLine(cx(i), y1, cx(i + 1), y2, linePaint)
            }
            data.forEachIndexed { i, g ->
                val rel = g.relativeToPar
                val py = baselineY - plotH * (rel / range)
                barPaint.color = colorFor(rel)
                canvas.drawCircle(cx(i), py, 5 * d, barPaint)
                labelPaint.color = fgColor
                if (rel < 0) canvas.drawText(RecordFormat.badgeText(rel), cx(i), py + 12 * fsd, labelPaint)
                else canvas.drawText(RecordFormat.badgeText(rel), cx(i), py - 9 * d, labelPaint)
                axisPaint.color = mutedColor
                canvas.drawText((i + 1).toString(), cx(i), xAxisY, axisPaint)
            }
        } else {
            data.forEachIndexed { i, g ->
                val rel = g.relativeToPar
                val c = cx(i)
                val stub = 3 * d
                if (rel >= 0) {
                    val barH = if (rel == 0) stub else plotH * (rel / range)
                    barPaint.color = if (rel == 0) mutedColor else overColor
                    canvas.drawRoundRect(c - barW / 2, baselineY - barH, c + barW / 2, baselineY, 4 * d, 4 * d, barPaint)
                    labelPaint.color = fgColor
                    canvas.drawText(RecordFormat.badgeText(rel), c, baselineY - barH - 5 * d, labelPaint)
                } else {
                    val barH = plotH * (-rel / range)
                    barPaint.color = underColor
                    canvas.drawRoundRect(c - barW / 2, baselineY, c + barW / 2, baselineY + barH, 4 * d, 4 * d, barPaint)
                    labelPaint.color = fgColor
                    canvas.drawText(RecordFormat.badgeText(rel), c, baselineY + barH + 12 * fsd, labelPaint)
                }
                axisPaint.color = mutedColor
                canvas.drawText((i + 1).toString(), c, xAxisY, axisPaint)
            }
        }
    }
}
