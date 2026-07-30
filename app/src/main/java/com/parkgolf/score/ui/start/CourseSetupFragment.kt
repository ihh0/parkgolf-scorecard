package com.parkgolf.score.ui.start

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentCourseSetupBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CourseSetupFragment : Fragment(R.layout.fragment_course_setup) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentCourseSetupBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_course_setup)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)

        var refresh: () -> Unit = {}
        val adapter = CourseToggleAdapter { refresh() }
        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCourses.adapter = adapter
        refresh = {
            val total = adapter.totalHoles()
            binding.tvTotalHoles.text = getString(R.string.total_holes, total)
            binding.btnNext.isEnabled = total > 0
        }
        refresh()

        viewLifecycleOwner.lifecycleScope.launch {
            var venueId = SelectionHolder.venueId
            val venues = repo.observeVenues().first()
            if (venueId == null) {
                val name = SelectionHolder.venueName
                venueId = venues.firstOrNull { it.name == name }?.id
            }
            if (venueId == null) { refresh(); return@launch }
            SelectionHolder.venueId = venueId
            if (SelectionHolder.venueName == null) {
                SelectionHolder.venueName = venues.firstOrNull { it.id == venueId }?.name
            }
            adapter.submit(repo.coursesForVenue(venueId))
            refresh()
        }

        binding.btnNext.setOnClickListener {
            if (SelectionHolder.chosenCourseIds.isNotEmpty()) {
                findNavController().navigate(R.id.playerSetupFragment)
            }
        }
    }
}
