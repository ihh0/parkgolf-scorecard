package com.parkgolf.score.ui.history

import android.os.Bundle
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentRoundDetailBinding
import com.parkgolf.score.domain.Scoring
import kotlinx.coroutines.launch

class RoundDetailFragment : Fragment(R.layout.fragment_round_detail) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentRoundDetailBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        val roundId = findNavController().previousBackStackEntry
            ?.savedStateHandle?.get<Long>("roundId") ?: run { findNavController().popBackStack(); return }

        viewLifecycleOwner.lifecycleScope.launch {
            val round = repo.getRound(roundId) ?: return@launch
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
                binding.tableGrid.addView(row)
            }
            val totals = TableRow(requireContext())
            totals.addView(cell("합계"))
            round.players.indices.forEach { p ->
                totals.addView(cell(Scoring.total(round.scores[p]).toString()))
            }
            binding.tableGrid.addView(totals)
        }

        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.delete_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        repo.deleteRound(roundId)
                        findNavController().popBackStack()
                    }
                }.show()
        }
    }
}
