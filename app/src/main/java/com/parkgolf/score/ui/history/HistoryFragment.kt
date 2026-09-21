package com.parkgolf.score.ui.history

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.databinding.FragmentHistoryBinding
import com.parkgolf.score.domain.model.Round
import kotlinx.coroutines.launch

class HistoryFragment : Fragment(R.layout.fragment_history) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHistoryBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_history)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)
        binding.rvRounds.layoutManager = LinearLayoutManager(requireContext())
        val adapter = HistoryAdapter(
            onView = { roundId ->
                findNavController().currentBackStackEntry?.savedStateHandle?.set("roundId", roundId)
                findNavController().navigate(R.id.roundDetailFragment)
            },
            onDelete = { round -> confirmDelete(repo, round) },
        )
        binding.rvRounds.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeCompletedRounds().collect { rounds ->
                adapter.submit(rounds)
                val empty = rounds.isEmpty()
                binding.emptyView.isVisible = empty
                binding.rvRounds.isVisible = !empty
                binding.statsCard.isVisible = !empty

                val stats = HistoryViewModel.computeStats(rounds, recentN = 10)
                binding.tvAvg.text = stats.recentAverage?.let { "%.1f".format(it) } ?: "-"
                binding.tvBest.text = stats.best?.toString() ?: "-"
                binding.tvWorst.text = stats.worst?.toString() ?: "-"
            }
        }
    }

    private fun confirmDelete(repo: ParkGolfRepository, round: Round) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch { repo.deleteRound(round.id) }
            }.show()
    }
}
