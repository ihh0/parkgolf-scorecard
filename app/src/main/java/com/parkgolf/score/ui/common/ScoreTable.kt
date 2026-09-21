package com.parkgolf.score.ui.common

import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.R
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.ui.hole.ParColors

/**
 * 라운드를 전체 점수표로 렌더한다. 인게임(홀 탭→편집)과 기록 상세(읽기 전용)가 공유.
 * @param onHoleClick null이면 읽기 전용(행 탭 없음).
 */
object ScoreTable {
    fun render(table: TableLayout, round: Round, onHoleClick: ((holeIndex: Int) -> Unit)? = null) {
        val ctx = table.context
        table.removeAllViews()
        val fg = MaterialColors.getColor(table, R.attr.parkForeground)
        val muted = MaterialColors.getColor(table, R.attr.parkMutedForeground)

        fun cell(text: String, bold: Boolean = false, color: Int = fg): TextView =
            TextView(ctx).apply {
                this.text = text; setPadding(28, 22, 28, 22); textSize = 15f
                gravity = Gravity.CENTER; setTextColor(color)
                if (bold) setTypeface(typeface, Typeface.BOLD)
            }

        val header = TableRow(ctx)
        header.addView(cell(ctx.getString(R.string.grid_hole_col), bold = true, color = muted))
        header.addView(cell(ctx.getString(R.string.grid_par_col), bold = true, color = muted))
        round.players.forEach { header.addView(cell(it, bold = true)) }
        table.addView(header)

        round.holes.forEachIndexed { h, hole ->
            val row = TableRow(ctx)
            row.addView(cell(hole.holeNo.toString(), bold = true))
            row.addView(cell(hole.par.toString(), color = muted))
            round.players.indices.forEach { p ->
                val s = round.scores[p][h]
                val color = if (s != null) ParColors.colorFor(ctx, s - hole.par) else fg
                row.addView(cell(s?.toString() ?: "-", color = color))
            }
            if (onHoleClick != null) row.setOnClickListener { onHoleClick(h) }
            table.addView(row)
        }

        val totals = TableRow(ctx)
        totals.addView(cell(ctx.getString(R.string.grid_total_row), bold = true, color = muted))
        totals.addView(cell(Scoring.parTotal(round.holes).toString(), bold = true, color = muted))
        round.players.indices.forEach { p ->
            val total = Scoring.total(round.scores[p])
            val rel = Scoring.relativeToPar(round.scores[p], round.holes)
            val container = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(28, 22, 28, 22)
            }
            container.addView(TextView(ctx).apply {
                text = total.toString(); textSize = 16f; gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD); setTextColor(fg)
            })
            container.addView(TextView(ctx).apply {
                text = Scoring.relationLabel(rel); textSize = 12f; gravity = Gravity.CENTER
                setTextColor(ParColors.colorFor(ctx, rel))
            })
            totals.addView(container)
        }
        table.addView(totals)
    }
}
