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
import com.parkgolf.score.ui.RoundSessionViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHomeBinding.bind(view)
        val repo = App.repo(requireActivity().application)

        binding.btnStart.setOnClickListener {
            findNavController().navigate(R.id.startFragment)
        }
        binding.btnHistory.setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }
        binding.btnMyCourses.setOnClickListener {
            findNavController().navigate(R.id.myCoursesFragment)
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
}
