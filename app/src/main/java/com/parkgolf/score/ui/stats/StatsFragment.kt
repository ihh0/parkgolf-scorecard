package com.parkgolf.score.ui.stats

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentStatsBinding
import com.parkgolf.score.domain.RecordFormat
import com.parkgolf.score.domain.StatsPeriod
import com.parkgolf.score.domain.model.Round
import kotlinx.coroutines.launch

class StatsFragment : Fragment(R.layout.fragment_stats) {
    private val vm: StatsViewModel by viewModels()
    private var rounds: List<Round> = emptyList()
    private var courseOptions: List<String> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentStatsBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_my_stats)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.tabRecent10.setOnClickListener { vm.period = StatsPeriod.RECENT_10; render(binding) }
        binding.tab3m.setOnClickListener { vm.period = StatsPeriod.THREE_MONTHS; render(binding) }
        binding.tabAll.setOnClickListener { vm.period = StatsPeriod.ALL; render(binding) }
        binding.btnCourseFilter.setOnClickListener { pickCourse(binding) }

        val repo = App.repo(requireActivity().application)
        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeCompletedRounds().collect { list ->
                rounds = list
                render(binding)
            }
        }
    }

    private fun pickCourse(binding: FragmentStatsBinding) {
        val items = (listOf(getString(R.string.stats_course_all)) + courseOptions).toTypedArray()
        val checked = if (vm.courseLabel == null) 0 else (courseOptions.indexOf(vm.courseLabel) + 1).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.stats_pick_course)
            .setSingleChoiceItems(items, checked) { dialog, which ->
                vm.courseLabel = if (which == 0) null else items[which]
                dialog.dismiss()
                render(binding)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun render(binding: FragmentStatsBinding) {
        var state = vm.compute(rounds, System.currentTimeMillis())
        // 선택했던 코스의 기록이 모두 사라지면 '전체 코스'로 되돌려 멈춤 방지.
        if (vm.courseLabel != null && vm.courseLabel !in state.courseOptions) {
            vm.courseLabel = null
            state = vm.compute(rounds, System.currentTimeMillis())
        }
        courseOptions = state.courseOptions

        styleTab(binding.tabRecent10, vm.period == StatsPeriod.RECENT_10)
        styleTab(binding.tab3m, vm.period == StatsPeriod.THREE_MONTHS)
        styleTab(binding.tabAll, vm.period == StatsPeriod.ALL)

        binding.btnCourseFilter.text = vm.courseLabel ?: getString(R.string.stats_course_all)

        val empty = state.graph.isEmpty()
        binding.summaryCard.isVisible = !empty
        binding.chartScroll.isVisible = !empty
        binding.emptyView.isVisible = empty

        val s = state.summary
        binding.tvCount.text = s.count.toString()
        binding.tvAvg.text = s.average?.let { "%+.1f".format(it) } ?: "-"
        binding.tvBest.text = s.best?.let { RecordFormat.badgeText(it) } ?: "-"
        binding.tvWorst.text = s.worst?.let { RecordFormat.badgeText(it) } ?: "-"

        binding.chart.setData(state.graph)
    }

    private fun styleTab(tab: TextView, selected: Boolean) {
        if (selected) {
            tab.setBackgroundResource(R.drawable.bg_pill_primary)
            tab.setTextColor(MaterialColors.getColor(tab, R.attr.parkPrimary))
        } else {
            tab.setBackgroundResource(R.drawable.bg_pill_muted)
            tab.setTextColor(MaterialColors.getColor(tab, R.attr.parkMutedForeground))
        }
    }
}
