package com.parkgolf.score.ui.courses

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.databinding.FragmentCourseEditBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CourseEditFragment : Fragment(R.layout.fragment_course_edit) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentCourseEditBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        val rawId = findNavController().previousBackStackEntry?.savedStateHandle?.get<Long>("editVenueId")
        val editVenueId: Long? = if (rawId == null || rawId == 0L) null else rawId

        val parAdapter = ParEditorAdapter()
        binding.rvPars.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPars.adapter = parAdapter
        parAdapter.setHoleCount(9)

        binding.etHoleCount.doAfterTextChanged { text ->
            val n = text?.toString()?.toIntOrNull() ?: return@doAfterTextChanged
            if (n in 1..27) parAdapter.setHoleCount(n)
        }

        if (editVenueId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val venue = repo.observeVenues().first().firstOrNull { it.id == editVenueId }
                if (venue != null) binding.etVenueName.setText(venue.name)
            }
        }

        binding.btnSave.setOnClickListener {
            val venueName = binding.etVenueName.text.toString().trim()
            if (venueName.isEmpty()) {
                binding.etVenueName.error = getString(R.string.venue_name_hint)
                return@setOnClickListener
            }
            val courseName = binding.etCourseName.text.toString().trim().ifEmpty { "A코스" }
            val pars = parAdapter.pars.toList()
            viewLifecycleOwner.lifecycleScope.launch {
                val vId = if (editVenueId != null) {
                    repo.upsertVenue(VenueEntity(id = editVenueId, name = venueName)); editVenueId
                } else {
                    repo.upsertVenue(VenueEntity(name = venueName))
                }
                repo.upsertCourse(CourseEntity(venueId = vId, name = courseName, pars = pars))
                findNavController().popBackStack()
            }
        }
    }
}
