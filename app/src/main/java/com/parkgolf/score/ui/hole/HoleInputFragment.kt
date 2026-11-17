package com.parkgolf.score.ui.hole

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentHoleInputBinding
import com.parkgolf.score.databinding.ViewPlayerScoreRowBinding
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.ui.RoundSessionViewModel
import com.parkgolf.score.ui.common.Haptics
import com.parkgolf.score.ui.common.confirmYesNo
import com.parkgolf.score.ui.common.onBackPressed
import kotlinx.coroutines.launch

class HoleInputFragment : Fragment(R.layout.fragment_hole_input) {
    private val session: RoundSessionViewModel by activityViewModels()
    private lateinit var holeVm: HoleInputViewModel
    private lateinit var repo: ParkGolfRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHoleInputBinding.bind(view)
        repo = App.repo(requireActivity().application)
        val round = session.round.value ?: run { findNavController().popBackStack(); return }
        binding.topBar.tvBarTitle.text = getString(R.string.title_game_play)
        val exitToHome = {
            confirmYesNo(R.string.confirm_exit_game) {
                findNavController().popBackStack(R.id.homeFragment, false)
            }
        }
        binding.topBar.btnBack.setOnClickListener { exitToHome() }
        onBackPressed { exitToHome() }
        holeVm = HoleInputViewModel(totalHoles = round.holes.size)

        val rows = round.players.indices.map { p ->
            val rowBinding = ViewPlayerScoreRowBinding.inflate(layoutInflater, binding.playerContainer, false)
            rowBinding.tvPlayer.text = round.players[p]
            binding.playerContainer.addView(rowBinding.root)
            rowBinding
        }
        round.players.indices.forEach { p ->
            rows[p].btnPlus.setOnClickListener {
                session.adjust(p, holeVm.holeIndex.value!!, +1); persist(); render(binding, rows)
            }
            rows[p].btnMinus.setOnClickListener {
                session.adjust(p, holeVm.holeIndex.value!!, -1); persist(); render(binding, rows)
            }
        }

        binding.btnNext.setOnClickListener {
            if (holeVm.isLastHole()) {
                Haptics.complete(requireContext())
                findNavController().navigate(R.id.roundSummaryFragment)
            } else {
                Haptics.next(requireContext())
                holeVm.next(); render(binding, rows)
            }
        }
        binding.btnPrev.setOnClickListener {
            Haptics.prev(requireContext())
            holeVm.prev(); render(binding, rows)
        }
        binding.btnGrid.setOnClickListener { findNavController().navigate(R.id.gridViewFragment) }

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Int>("jumpToHole")?.observe(viewLifecycleOwner) { idx ->
                holeVm.goTo(idx); render(binding, rows)
            }

        render(binding, rows)
    }

    private fun persist() {
        val r = session.round.value ?: return
        viewLifecycleOwner.lifecycleScope.launch { repo.saveRound(r) }
    }

    private fun render(binding: FragmentHoleInputBinding, rows: List<ViewPlayerScoreRowBinding>) {
        val h = holeVm.holeIndex.value!!
        if (session.commitDefaultForHole(h)) persist()
        val round = session.round.value ?: return
        val hole = round.holes[h]
        val total = round.holes.size

        binding.tvVenueChip.text = round.venueName
        binding.tvCourse.text = hole.courseName
        binding.tvHole.text = getString(R.string.hole_label, hole.holeNo)
        binding.tvPar.text = getString(R.string.par_label, hole.par)
        binding.tvHoleCounter.text = getString(R.string.hole_counter, h + 1, total)
        binding.btnPrev.isEnabled = h > 0
        binding.btnNext.text =
            if (holeVm.isLastHole()) getString(R.string.game_finish) else getString(R.string.next_hole_plain)

        binding.holeProgress.setProgress(total, h)

        round.players.indices.forEach { p ->
            val score = round.scores[p][h]
            rows[p].tvScore.text = score?.toString() ?: "-"
            rows[p].tvCumulative.text =
                getString(R.string.cumulative_strokes, Scoring.runningStrokes(round.scores[p], h))
            val diff = (score ?: hole.par) - hole.par
            rows[p].tvBadge.text = Scoring.relationLabel(diff)
            rows[p].tvBadge.setTextColor(ParColors.colorFor(requireContext(), diff))
            rows[p].tvBadge.setBackgroundResource(
                if (diff < 0) R.drawable.bg_pill_primary else R.drawable.bg_pill_secondary
            )
        }
    }
}
