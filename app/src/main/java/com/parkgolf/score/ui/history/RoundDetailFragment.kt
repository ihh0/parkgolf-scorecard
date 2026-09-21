package com.parkgolf.score.ui.history

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentRoundDetailBinding
import com.parkgolf.score.ui.common.ScoreTable
import kotlinx.coroutines.launch

class RoundDetailFragment : Fragment(R.layout.fragment_round_detail) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentRoundDetailBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_round_detail)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)
        val roundId = findNavController().previousBackStackEntry
            ?.savedStateHandle?.get<Long>("roundId") ?: run { findNavController().popBackStack(); return }

        viewLifecycleOwner.lifecycleScope.launch {
            val round = repo.getRound(roundId) ?: run { findNavController().popBackStack(); return@launch }
            ScoreTable.render(binding.tableGrid, round)  // 읽기 전용(onHoleClick 없음)
        }
    }
}
