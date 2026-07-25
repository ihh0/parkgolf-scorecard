package com.parkgolf.score.ui.hole

import android.content.Context
import androidx.core.content.ContextCompat
import com.parkgolf.score.R
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.ParRelation

object ParColors {
    fun colorFor(ctx: Context, diff: Int): Int {
        val res = when (Scoring.relation(diff)) {
            ParRelation.UNDER -> R.color.under_par
            ParRelation.EVEN -> R.color.even_par
            ParRelation.OVER -> R.color.over_par
        }
        return ContextCompat.getColor(ctx, res)
    }
    fun label(diff: Int): String = Scoring.relationLabel(diff)
}
