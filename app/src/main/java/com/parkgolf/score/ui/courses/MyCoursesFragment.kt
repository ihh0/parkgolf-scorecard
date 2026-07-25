package com.parkgolf.score.ui.courses

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentMyCoursesBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MyCoursesFragment : Fragment(R.layout.fragment_my_courses) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentMyCoursesBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        binding.rvVenues.layoutManager = LinearLayoutManager(requireContext())
        val adapter = VenueAdapter { venueId ->
            findNavController().currentBackStackEntry?.savedStateHandle?.set("editVenueId", venueId)
            findNavController().navigate(R.id.courseEditFragment)
        }
        binding.rvVenues.adapter = adapter

        binding.btnAddVenue.setOnClickListener {
            findNavController().currentBackStackEntry?.savedStateHandle?.set("editVenueId", 0L)
            findNavController().navigate(R.id.courseEditFragment)
        }

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
}
