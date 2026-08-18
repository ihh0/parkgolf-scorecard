package com.parkgolf.score.ui.summary

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentRoundSummaryBinding
import com.parkgolf.score.databinding.ItemRankRowBinding
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.RoundStatus
import com.parkgolf.score.ui.RoundSessionViewModel
import com.parkgolf.score.ui.common.onBackPressed
import kotlinx.coroutines.launch

class RoundSummaryFragment : Fragment(R.layout.fragment_round_summary) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentRoundSummaryBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_summary)
        val repo = App.repo(requireActivity().application)
        val round = session.round.value ?: run { findNavController().popBackStack(); return }

        // '뒤로'는 점수 기록의 마지막 홀로 돌아간다(1번 홀이 아니라).
        val backToLastHole = {
            findNavController().previousBackStackEntry
                ?.savedStateHandle?.set("jumpToHole", round.holes.size - 1)
            findNavController().popBackStack()
        }
        binding.topBar.btnBack.setOnClickListener { backToLastHole() }
        onBackPressed { backToLastHole() }

        val course = round.holes.map { it.courseName }.distinct().firstOrNull() ?: ""
        binding.tvVenue.text = if (course.isBlank()) round.venueName else "${round.venueName} · $course"
        val parTotal = Scoring.parTotal(round.holes)

        val medalColors = intArrayOf(R.color.medal_gold, R.color.medal_silver, R.color.medal_bronze)

        SummaryViewModel.ranking(round).forEach { row ->
            val item = ItemRankRowBinding.inflate(layoutInflater, binding.rankContainer, false)
            item.tvPlayer.text = row.player
            item.tvTotal.text = row.total.toString()

            val relText = when {
                row.relative > 0 -> "+${row.relative}"
                row.relative == 0 -> getString(R.string.even_label)
                else -> row.relative.toString()
            }
            item.tvRelative.text = getString(R.string.par_versus, parTotal, relText)

            if (row.rank in 1..3) {
                val c = ContextCompat.getColor(requireContext(), medalColors[row.rank - 1])
                item.ivMedal.isVisible = true
                item.tvRankNum.backgroundTintList = android.content.res.ColorStateList.valueOf(c)
                item.tvRankNum.text = ""
                item.tvRankLabel.isVisible = true
                item.tvRankLabel.text = getString(R.string.rank_suffix, row.rank)
                item.tvRankLabel.setTextColor(c)
            } else {
                item.ivMedal.isVisible = false
                item.tvRankNum.text = row.rank.toString()
                item.tvRankLabel.isVisible = false
            }
            if (row.rank == 1) {
                item.rankCard.setBackgroundResource(R.drawable.bg_pill_primary)
            }
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
