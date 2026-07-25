package com.parkgolf.score.ui.start

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentStartBinding
import kotlinx.coroutines.launch

class StartFragment : Fragment(R.layout.fragment_start) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentStartBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPresets.layoutManager = LinearLayoutManager(requireContext())

        val recentAdapter = RecentCourseAdapter { recent ->
            SelectionHolder.reset()
            SelectionHolder.venueName = recent.venueName
            findNavController().navigate(R.id.courseSetupFragment)
        }
        binding.rvRecent.adapter = recentAdapter

        val presetAdapter = PresetCourseAdapter { venueId ->
            SelectionHolder.reset()
            SelectionHolder.venueId = venueId
            findNavController().navigate(R.id.courseSetupFragment)
        }
        binding.rvPresets.adapter = presetAdapter

        binding.btnNewCourse.setOnClickListener {
            SelectionHolder.reset()
            findNavController().navigate(R.id.courseEditFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val recents = StartViewModel.deriveRecentCourses(repo.completedRounds(), limit = 5)
            recentAdapter.submit(recents)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeVenues().collect { venues -> presetAdapter.submit(venues) }
        }
    }
}
