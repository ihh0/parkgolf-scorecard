package com.parkgolf.score.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentHomeBinding
import com.parkgolf.score.databinding.ViewHomeMenuItemBinding
import com.parkgolf.score.ui.RoundSessionViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHomeBinding.bind(view)
        val repo = App.repo(requireActivity().application)

        bindItem(
            binding.itemGameStart, R.drawable.ic_play_circle,
            R.string.menu_game_start, R.string.menu_game_start_desc,
        ) { findNavController().navigate(R.id.startFragment) }

        bindItem(
            binding.itemGameRecords, R.drawable.ic_clipboard_list,
            R.string.menu_game_records, R.string.menu_game_records_desc,
        ) { findNavController().navigate(R.id.historyFragment) }

        bindItem(
            binding.itemCourseManagement, R.drawable.ic_layout_list,
            R.string.menu_course_management, R.string.menu_course_management_desc,
        ) { findNavController().navigate(R.id.myCoursesFragment) }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val current = repo.currentInProgressRound()
            if (current != null && session.round.value == null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.resume_round)
                    .setPositiveButton(R.string.resume) { _, _ ->
                        session.startRound(current)
                        findNavController().navigate(R.id.holeInputFragment)
                    }
                    .setNegativeButton(R.string.discard) { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch { repo.deleteRound(current.id) }
                    }
                    .show()
            }
        }
    }

    private fun bindItem(
        item: ViewHomeMenuItemBinding,
        iconRes: Int,
        titleRes: Int,
        descRes: Int,
        onClick: () -> Unit,
    ) {
        item.ivIcon.setImageResource(iconRes)
        item.tvTitle.setText(titleRes)
        item.tvDesc.setText(descRes)
        item.root.setOnClickListener { onClick() }
    }
}
