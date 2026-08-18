package com.parkgolf.score.ui.grid

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentGridViewBinding
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.ui.RoundSessionViewModel
import com.parkgolf.score.ui.hole.ParColors

class GridViewFragment : Fragment(R.layout.fragment_grid_view) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentGridViewBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_grid)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val round = session.round.value ?: run { findNavController().popBackStack(); return }

        val fg = MaterialColors.getColor(requireView(), R.attr.parkForeground)
        val muted = MaterialColors.getColor(requireView(), R.attr.parkMutedForeground)

        fun cell(text: String, bold: Boolean = false, color: Int = fg): TextView =
            TextView(requireContext()).apply {
                this.text = text; setPadding(28, 22, 28, 22); textSize = 15f
                gravity = Gravity.CENTER; setTextColor(color)
                if (bold) setTypeface(typeface, Typeface.BOLD)
            }

        val header = TableRow(requireContext())
        header.addView(cell(getString(R.string.grid_hole_col), bold = true, color = muted))
        header.addView(cell(getString(R.string.grid_par_col), bold = true, color = muted))
        round.players.forEach { header.addView(cell(it, bold = true)) }
        binding.tableGrid.addView(header)

        round.holes.forEachIndexed { h, hole ->
            val row = TableRow(requireContext())
            row.addView(cell(hole.holeNo.toString(), bold = true))
            row.addView(cell(hole.par.toString(), color = muted))
            round.players.indices.forEach { p ->
                val s = round.scores[p][h]
                val color = if (s != null) ParColors.colorFor(requireContext(), s - hole.par) else fg
                row.addView(cell(s?.toString() ?: "-", color = color))
            }
            row.setOnClickListener {
                findNavController().previousBackStackEntry?.savedStateHandle?.set("jumpToHole", h)
                findNavController().popBackStack()
            }
            binding.tableGrid.addView(row)
        }

        val totals = TableRow(requireContext())
        totals.addView(cell(getString(R.string.grid_total_row), bold = true, color = muted))
        totals.addView(cell(Scoring.parTotal(round.holes).toString(), bold = true, color = muted))
        round.players.indices.forEach { p ->
            val total = Scoring.total(round.scores[p])
            val rel = Scoring.relativeToPar(round.scores[p], round.holes)
            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(28, 22, 28, 22)
            }
            container.addView(TextView(requireContext()).apply {
                text = total.toString(); textSize = 16f; gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD); setTextColor(fg)
            })
            container.addView(TextView(requireContext()).apply {
                text = Scoring.relationLabel(rel); textSize = 12f; gravity = Gravity.CENTER
                setTextColor(ParColors.colorFor(requireContext(), rel))
            })
            totals.addView(container)
        }
        binding.tableGrid.addView(totals)
    }
}
