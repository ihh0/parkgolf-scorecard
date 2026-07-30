package com.parkgolf.score.ui.history

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch

class HistoryFragment : Fragment(R.layout.fragment_history) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHistoryBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_history)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)
        binding.rvRounds.layoutManager = LinearLayoutManager(requireContext())
        val adapter = HistoryAdapter { roundId ->
            findNavController().currentBackStackEntry?.savedStateHandle?.set("roundId", roundId)
            findNavController().navigate(R.id.roundDetailFragment)
        }
        binding.rvRounds.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeCompletedRounds().collect { rounds ->
                adapter.submit(rounds)
                val stats = HistoryViewModel.computeStats(rounds, recentN = 10)
                binding.tvAvg.text = stats.recentAverage?.let { "평균 %.1f타".format(it) } ?: "평균 -"
                binding.tvBest.text = stats.best?.let { "최저 ${it}타" } ?: "최저 -"
                binding.tvWorst.text = stats.worst?.let { "최고 ${it}타" } ?: "최고 -"
            }
        }
    }
}
