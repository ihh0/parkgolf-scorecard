package com.parkgolf.score.ui.summary

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentRoundSummaryBinding
import com.parkgolf.score.databinding.ItemRankRowBinding
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.RoundStatus
import com.parkgolf.score.ui.RoundSessionViewModel
import com.parkgolf.score.ui.hole.ParColors
import kotlinx.coroutines.launch

class RoundSummaryFragment : Fragment(R.layout.fragment_round_summary) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentRoundSummaryBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_summary)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)
        val round = session.round.value ?: run { findNavController().popBackStack(); return }

        binding.tvVenue.text = round.venueName
        SummaryViewModel.ranking(round).forEach { row ->
            val item = ItemRankRowBinding.inflate(layoutInflater, binding.rankContainer, false)
            item.tvRank.text = getString(R.string.rank_suffix, row.rank)
            item.tvPlayer.text = row.player
            item.tvTotal.text = getString(R.string.strokes_suffix, row.total)
            item.tvRelative.text = Scoring.relationLabel(row.relative)
            item.tvRelative.setTextColor(ParColors.colorFor(requireContext(), row.relative))
            binding.rankContainer.addView(item.root)
        }

        val completed = round.copy(status = RoundStatus.COMPLETED)
        viewLifecycleOwner.lifecycleScope.launch { repo.saveRound(completed) }
        session.startRound(completed)

        binding.btnDone.setOnClickListener {
            session.round.value = null
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }
}
