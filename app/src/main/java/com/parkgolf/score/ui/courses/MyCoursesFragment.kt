package com.parkgolf.score.ui.courses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.databinding.FragmentMyCoursesBinding
import com.parkgolf.score.databinding.ViewCourseRowBinding
import com.parkgolf.score.databinding.ViewCourseVenueCardBinding
import com.parkgolf.score.domain.CourseFormat
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MyCoursesFragment : Fragment(R.layout.fragment_my_courses) {
    /** 비어있으면 전체 펼침. 접힌 구장 id만 담는다(새 구장은 기본 펼침). */
    private val collapsed = mutableSetOf<Long>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentMyCoursesBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_my_courses)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)

        binding.btnNewTemplate.setOnClickListener { openWizard(0L, 0L) }
        binding.btnEmptyAdd.setOnClickListener { openWizard(0L, 0L) }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(repo.observeVenues(), repo.observeCourses()) { venues, courses ->
                venues to courses
            }.collect { (venues, courses) ->
                build(binding, repo, venues, courses)
            }
        }
    }

    private fun build(
        binding: FragmentMyCoursesBinding,
        repo: ParkGolfRepository,
        venues: List<VenueEntity>,
        courses: List<CourseEntity>,
    ) {
        val empty = venues.isEmpty()
        binding.emptyView.isVisible = empty
        binding.scrollContent.isVisible = !empty
        binding.containerVenues.removeAllViews()
        if (empty) return

        val inflater = LayoutInflater.from(requireContext())
        val byVenue = courses.groupBy { it.venueId }
        venues.forEach { venue ->
            val vCourses = byVenue[venue.id].orEmpty()
            val card = ViewCourseVenueCardBinding.inflate(inflater, binding.containerVenues, false)
            card.tvVenueName.text = venue.name
            card.tvCourseCount.text = getString(R.string.course_count, vCourses.size)

            val expanded = venue.id !in collapsed
            card.btnToggle.setImageResource(
                if (expanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
            val toggle = {
                if (!collapsed.add(venue.id)) collapsed.remove(venue.id)
                build(binding, repo, venues, courses)
            }
            card.headerClick.setOnClickListener { toggle() }
            card.btnToggle.setOnClickListener { toggle() }
            card.btnDeleteVenue.setOnClickListener { confirmDeleteVenue(repo, venue) }

            card.topDivider.isVisible = expanded && vCourses.isNotEmpty()
            card.coursesContainer.isVisible = expanded
            card.coursesContainer.removeAllViews()
            if (expanded) {
                vCourses.forEachIndexed { i, course ->
                    val row = ViewCourseRowBinding.inflate(inflater, card.coursesContainer, false)
                    row.tvCourseName.text = course.name
                    row.tvHolesPar.text = CourseFormat.holesPar(course.pars)
                    row.btnEditCourse.setOnClickListener { openWizard(venue.id, course.id) }
                    row.btnDeleteCourse.setOnClickListener { confirmDeleteCourse(repo, venue, course) }
                    row.divider.isVisible = i < vCourses.size - 1
                    card.coursesContainer.addView(row.root)
                }
            }
            binding.containerVenues.addView(card.root)
        }
    }

    private fun confirmDeleteVenue(repo: ParkGolfRepository, venue: VenueEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_venue_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch { repo.deleteVenueWithCourses(venue.id) }
            }.show()
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

    private fun openWizard(venueId: Long, courseId: Long) {
        findNavController().currentBackStackEntry?.savedStateHandle?.apply {
            set("wizardVenueId", venueId)
            set("wizardCourseId", courseId)
        }
        findNavController().navigate(R.id.courseWizardFragment)
    }
}
