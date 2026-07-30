package com.parkgolf.score.ui.courses

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.databinding.FragmentMyCoursesBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MyCoursesFragment : Fragment(R.layout.fragment_my_courses) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentMyCoursesBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_my_courses)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)
        binding.rvVenues.layoutManager = LinearLayoutManager(requireContext())
        val adapter = VenueAdapter { venueId -> onVenueTapped(repo, venueId) }
        binding.rvVenues.adapter = adapter

        binding.btnAddVenue.setOnClickListener { openWizard(venueId = 0L, courseId = 0L) }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(repo.observeVenues(), repo.observeCourses()) { venues, courses ->
                venues.map { v ->
                    val summary = courses.filter { it.venueId == v.id }.joinToString("·") { it.name }
                    VenueRow(v.id, v.name, summary)
                }
            }.collect { rows ->
                adapter.submit(rows)
                binding.tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun onVenueTapped(repo: ParkGolfRepository, venueId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val courses = repo.coursesForVenue(venueId)
            when {
                courses.isEmpty() -> openWizard(venueId, 0L)
                courses.size == 1 -> openWizard(venueId, courses[0].id)
                else -> {
                    val names = courses.map { it.name }.toTypedArray()
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.pick_course_to_edit)
                        .setItems(names) { _, which -> openWizard(venueId, courses[which].id) }
                        .show()
                }
            }
        }
    }

    private fun openWizard(venueId: Long, courseId: Long) {
        findNavController().currentBackStackEntry?.savedStateHandle?.apply {
            set("wizardVenueId", venueId)
            set("wizardCourseId", courseId)
        }
        findNavController().navigate(R.id.courseWizardFragment)
    }
}
