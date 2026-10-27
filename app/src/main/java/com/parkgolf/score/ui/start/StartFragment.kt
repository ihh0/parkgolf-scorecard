package com.parkgolf.score.ui.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.databinding.FragmentStartBinding
import com.parkgolf.score.databinding.ViewStartSavedCourseRowBinding
import com.parkgolf.score.databinding.ViewStartVenueCardBinding
import com.parkgolf.score.domain.CourseFormat
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
            findNavController().navigate(R.id.createCourseFragment)
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
        val repo = App.repo(requireActivity().application)
        courses.forEachIndexed { idx, course ->
            val row = ViewStartSavedCourseRowBinding.inflate(inflater, card.coursesContainer, false)
            row.tvCourseName.text = course.name
            row.tvHolesPar.text = CourseFormat.holesPar(course.pars)
            row.divider.isGone = idx == courses.lastIndex
            row.rowCourse.setOnClickListener { selectCourse(venue.id, venue.name, course.id) }
            row.btnEditCourse.setOnClickListener { openWizard(venue.id, course.id) }
            row.btnDeleteCourse.setOnClickListener { confirmDeleteCourse(repo, venue, course) }
            card.coursesContainer.addView(row.root)
        }
        binding.containerVenues.addView(card.root)
    }

    private fun openWizard(venueId: Long, courseId: Long) {
        findNavController().currentBackStackEntry?.savedStateHandle?.apply {
            set("wizardVenueId", venueId)
            set("wizardCourseId", courseId)
        }
        findNavController().navigate(R.id.courseWizardFragment)
    }

    private fun confirmDeleteCourse(repo: ParkGolfRepository, venue: VenueEntity, course: CourseEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_course_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repo.deleteCourse(course)
                    if (repo.coursesForVenue(venue.id).isEmpty()) repo.deleteVenue(venue)
                }
            }.show()
    }

    private fun selectCourse(venueId: Long, venueName: String, courseId: Long) {
        SelectionHolder.reset()
        SelectionHolder.venueId = venueId
        SelectionHolder.venueName = venueName
        SelectionHolder.chosenCourseIds.add(courseId)
        findNavController().navigate(R.id.playerSetupFragment)
    }
}
