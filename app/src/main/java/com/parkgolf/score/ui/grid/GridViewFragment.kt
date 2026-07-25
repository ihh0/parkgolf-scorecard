package com.parkgolf.score.ui.grid

import android.os.Bundle
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentGridViewBinding
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.ui.RoundSessionViewModel

class GridViewFragment : Fragment(R.layout.fragment_grid_view) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentGridViewBinding.bind(view)
        val round = session.round.value ?: run { findNavController().popBackStack(); return }

        fun cell(text: String) = TextView(requireContext()).apply {
            this.text = text; setPadding(24, 20, 24, 20); textSize = 16f
        }

        val header = TableRow(requireContext())
        header.addView(cell("홀"))
        round.players.forEach { header.addView(cell(it)) }
        binding.tableGrid.addView(header)

        round.holes.forEachIndexed { h, hole ->
            val row = TableRow(requireContext())
            row.addView(cell("${hole.holeNo}(파${hole.par})"))
            round.players.indices.forEach { p ->
                row.addView(cell(round.scores[p][h]?.toString() ?: "-"))
            }
            row.setOnClickListener {
                findNavController().previousBackStackEntry
                    ?.savedStateHandle?.set("jumpToHole", h)
                findNavController().popBackStack()
            }
            binding.tableGrid.addView(row)
        }

        val totals = TableRow(requireContext())
        totals.addView(cell("합계"))
        round.players.indices.forEach { p ->
            totals.addView(cell(Scoring.total(round.scores[p]).toString()))
        }
        binding.tableGrid.addView(totals)
    }
}
