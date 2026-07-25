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
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.ui.RoundSessionViewModel
import kotlinx.coroutines.launch

class HoleInputFragment : Fragment(R.layout.fragment_hole_input) {
    private val session: RoundSessionViewModel by activityViewModels()
    private lateinit var holeVm: HoleInputViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHoleInputBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        val round = session.round.value ?: run { findNavController().popBackStack(); return }
        holeVm = HoleInputViewModel(totalHoles = round.holes.size)

        val rows = round.players.indices.map { p ->
            val rowBinding = ViewPlayerScoreRowBinding.inflate(layoutInflater, binding.playerContainer, false)
            rowBinding.tvPlayer.text = round.players[p]
            binding.playerContainer.addView(rowBinding.root)
            rowBinding
        }
        round.players.indices.forEach { p ->
            rows[p].btnPlus.setOnClickListener {
                session.adjust(p, holeVm.holeIndex.value!!, +1); persist(repo); render(binding, rows)
            }
            rows[p].btnMinus.setOnClickListener {
                session.adjust(p, holeVm.holeIndex.value!!, -1); persist(repo); render(binding, rows)
            }
        }

        binding.btnNext.setOnClickListener {
            if (holeVm.isLastHole()) {
                findNavController().navigate(R.id.roundSummaryFragment)
            } else { holeVm.next(); render(binding, rows) }
        }
        binding.btnPrev.setOnClickListener { holeVm.prev(); render(binding, rows) }
        binding.btnGrid.setOnClickListener { findNavController().navigate(R.id.gridViewFragment) }

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Int>("jumpToHole")?.observe(viewLifecycleOwner) { idx ->
                holeVm.goTo(idx); render(binding, rows)
            }

        render(binding, rows)
    }

    private fun persist(repo: com.parkgolf.score.data.ParkGolfRepository) {
        val r = session.round.value ?: return
        viewLifecycleOwner.lifecycleScope.launch { repo.saveRound(r) }
    }

    private fun render(binding: FragmentHoleInputBinding, rows: List<ViewPlayerScoreRowBinding>) {
        val round = session.round.value ?: return
        val h = holeVm.holeIndex.value!!
        val hole = round.holes[h]
        binding.tvVenue.text = round.venueName
        binding.tvHole.text = getString(R.string.hole_label, hole.holeNo)
        binding.tvPar.text = getString(R.string.par_label, hole.par)
        binding.btnNext.text = if (holeVm.isLastHole()) getString(R.string.save) else getString(R.string.next_hole)

        round.players.indices.forEach { p ->
            val score = round.scores[p][h]
            rows[p].tvScore.text = score?.toString() ?: "-"
        }
        val cum0 = Scoring.relativeToPar(round.scores[0], round.holes)
        binding.tvCumulative.text = getString(R.string.cumulative_label, Scoring.relationLabel(cum0))
        binding.tvCumulative.setTextColor(ParColors.colorFor(requireContext(), cum0))
    }
}
