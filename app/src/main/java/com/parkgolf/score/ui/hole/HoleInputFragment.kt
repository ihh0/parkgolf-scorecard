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
import com.parkgolf.score.ui.common.confirmYesNo
import com.parkgolf.score.ui.common.onBackPressed
import kotlinx.coroutines.launch

class HoleInputFragment : Fragment(R.layout.fragment_hole_input) {
    private val session: RoundSessionViewModel by activityViewModels()
    private lateinit var holeVm: HoleInputViewModel
    private val dots = mutableListOf<View>()
    private var dotSize = 0
    private var dotWide = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHoleInputBinding.bind(view)
        val repo = App.repo(requireActivity().application)
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
                session.adjust(p, holeVm.holeIndex.value!!, +1); persist(repo); render(binding, rows)
            }
            rows[p].btnMinus.setOnClickListener {
                session.adjust(p, holeVm.holeIndex.value!!, -1); persist(repo); render(binding, rows)
            }
        }

        dotSize = resources.getDimensionPixelSize(R.dimen.dot_size)
        dotWide = resources.getDimensionPixelSize(R.dimen.dot_current_width)
        round.holes.indices.forEach { idx ->
            val dot = View(requireContext())
            val lp = android.widget.LinearLayout.LayoutParams(dotSize, dotSize)
            lp.marginEnd = (dotSize * 0.7).toInt()
            dot.layoutParams = lp
            dot.setOnClickListener { holeVm.goTo(idx); render(binding, rows) }
            binding.dotsContainer.addView(dot)
            dots.add(dot)
        }

        binding.btnNext.setOnClickListener {
            if (holeVm.isLastHole()) findNavController().navigate(R.id.roundSummaryFragment)
            else { holeVm.next(); render(binding, rows) }
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
        val total = round.holes.size

        binding.tvVenueChip.text = round.venueName
        binding.tvCourse.text = hole.courseName
        binding.tvHole.text = getString(R.string.hole_label, hole.holeNo)
        binding.tvPar.text = getString(R.string.par_label, hole.par)
        binding.tvHoleCounter.text = getString(R.string.hole_counter, h + 1, total)
        binding.btnPrev.isEnabled = h > 0
        binding.btnNext.text =
            if (holeVm.isLastHole()) getString(R.string.game_finish) else getString(R.string.next_hole_plain)

        dots.forEachIndexed { idx, dot ->
            val bg = when {
                idx == h -> R.drawable.bg_dot_current
                idx < h -> R.drawable.bg_dot_done
                else -> R.drawable.bg_dot_future
            }
            dot.setBackgroundResource(bg)
            val lp = dot.layoutParams
            lp.width = if (idx == h) dotWide else dotSize
            dot.layoutParams = lp
        }

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
