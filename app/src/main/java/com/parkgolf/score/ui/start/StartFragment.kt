package com.parkgolf.score.ui.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.databinding.FragmentStartBinding
import com.parkgolf.score.databinding.ViewStartCourseRowBinding
import com.parkgolf.score.databinding.ViewStartVenueCardBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StartFragment : Fragment(R.layout.fragment_start) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentStartBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_start)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)

        binding.btnNewCourse.setOnClickListener {
            SelectionHolder.reset()
            findNavController().currentBackStackEntry?.savedStateHandle?.apply {
                set("wizardVenueId", 0L)
                set("wizardCourseId", 0L)
            }
            findNavController().navigate(R.id.courseWizardFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val recents = StartViewModel.deriveRecentCourses(repo.completedRounds(), limit = 1)
            val recent = recents.firstOrNull()
            val venues = repo.observeVenues().first()
            val resolved = recent?.let { r ->
                val venue = venues.firstOrNull { it.name == r.venueName }
                val course = venue?.let { repo.coursesForVenue(it.id).firstOrNull { c -> c.name == r.courseName } }
                if (venue != null && course != null) Triple(venue, course, r) else null
            }
            if (resolved == null) {
                binding.sectionRecent.isGone = true
            } else {
                val (venue, course, r) = resolved
                binding.sectionRecent.isGone = false
                val fmt = SimpleDateFormat("M월 d일", Locale.KOREA)
                binding.tvRecentDate.text =
                    getString(R.string.recent_last_used, fmt.format(Date(r.lastPlayed)))
                binding.tvRecentVenue.text = venue.name
                binding.tvRecentCourse.text = course.name
                binding.cardRecent.setOnClickListener {
                    selectCourse(venue.id, venue.name, course.id)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeVenues().collect { venues ->
                binding.containerVenues.removeAllViews()
                val inflater = LayoutInflater.from(requireContext())
                for (venue in venues) {
                    val courses = repo.coursesForVenue(venue.id)
                    if (courses.isEmpty()) continue
                    addVenueCard(inflater, binding, venue, courses)
                }
            }
        }
    }

    private fun addVenueCard(
        inflater: LayoutInflater,
        binding: FragmentStartBinding,
        venue: VenueEntity,
        courses: List<CourseEntity>,
    ) {
        val card = ViewStartVenueCardBinding.inflate(inflater, binding.containerVenues, false)
        card.tvVenueName.text = venue.name
        courses.forEachIndexed { idx, course ->
            val row = ViewStartCourseRowBinding.inflate(inflater, card.coursesContainer, false)
            row.tvCourseName.text = course.name
            row.tvHoleCount.text = getString(R.string.hole_count, course.pars.size)
            row.divider.isGone = idx == courses.lastIndex
            row.rowCourse.setOnClickListener { selectCourse(venue.id, venue.name, course.id) }
            card.coursesContainer.addView(row.root)
        }
        binding.containerVenues.addView(card.root)
    }

    private fun selectCourse(venueId: Long, venueName: String, courseId: Long) {
        SelectionHolder.reset()
        SelectionHolder.venueId = venueId
        SelectionHolder.venueName = venueName
        SelectionHolder.chosenCourseIds.add(courseId)
        findNavController().navigate(R.id.playerSetupFragment)
    }
}
