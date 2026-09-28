package com.parkgolf.score.ui.start

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.databinding.FragmentStartBinding
import com.parkgolf.score.databinding.ViewNearbyVenueCardBinding
import com.parkgolf.score.databinding.ViewStartCourseRowBinding
import com.parkgolf.score.databinding.ViewStartVenueCardBinding
import com.parkgolf.score.ui.nearby.LocationProvider
import com.parkgolf.score.ui.nearby.NearbyGeo
import com.parkgolf.score.ui.nearby.NearbyRepository
import com.parkgolf.score.ui.nearby.NearbyVenue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StartFragment : Fragment(R.layout.fragment_start) {
    private lateinit var bindingRef: FragmentStartBinding
    private val locPerm = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) loadNearby()
        else showNearbyDenied(permanent = !shouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_FINE_LOCATION))
    }

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

        setupNearby(binding)
    }

    private fun setupNearby(binding: FragmentStartBinding) {
        bindingRef = binding
        binding.btnFindNearby.setOnClickListener { requestNearby() }
        binding.btnNearbyRetry.setOnClickListener { requestNearby() }
        binding.btnNearbySettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().packageName, null)))
        }
    }

    private fun hasLocationPerm(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun requestNearby() {
        if (hasLocationPerm()) loadNearby()
        else locPerm.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun nearbyState(statusText: String?, showRetry: Boolean, showSettings: Boolean) {
        val b = bindingRef
        b.btnFindNearby.isVisible = false
        b.tvNearbyStatus.isVisible = statusText != null
        b.tvNearbyStatus.text = statusText ?: ""
        b.nearbyActions.isVisible = showRetry || showSettings
        b.btnNearbyRetry.isVisible = showRetry
        b.btnNearbySettings.isVisible = showSettings
    }

    private fun showNearbyDenied(permanent: Boolean) {
        bindingRef.nearbyContainer.removeAllViews()
        nearbyState(getString(R.string.nearby_denied), showRetry = !permanent, showSettings = permanent)
    }

    private fun loadNearby() {
        nearbyState(getString(R.string.nearby_loading), showRetry = false, showSettings = false)
        bindingRef.nearbyContainer.removeAllViews()
        viewLifecycleOwner.lifecycleScope.launch {
            val loc = LocationProvider.current(requireContext().applicationContext)
            if (loc == null) {
                nearbyState(getString(R.string.nearby_error), showRetry = true, showSettings = false)
                return@launch
            }
            val venues = withContext(Dispatchers.IO) {
                NearbyRepository.load(requireContext().applicationContext)
            }
            val results = NearbyGeo.nearest(venues, loc.latitude, loc.longitude, limit = 10)
            if (results.isEmpty()) {
                nearbyState(getString(R.string.nearby_empty), showRetry = true, showSettings = false)
                return@launch
            }
            nearbyState(null, showRetry = false, showSettings = false)
            renderNearby(results)
        }
    }

    private fun renderNearby(results: List<Pair<NearbyVenue, Double>>) {
        val b = bindingRef
        val inflater = LayoutInflater.from(requireContext())
        val primary = MaterialColors.getColor(requireView(), R.attr.parkPrimary)
        for ((venue, km) in results) {
            val card = ViewNearbyVenueCardBinding.inflate(inflater, b.nearbyContainer, false)
            card.tvVenueName.text = venue.name
            card.tvDistance.text = NearbyGeo.formatDistance(
                km, getString(R.string.nearby_distance_m), getString(R.string.nearby_distance_km))
            card.tvDistance.setTextColor(primary)
            venue.courses.forEachIndexed { idx, course ->
                val row = ViewStartCourseRowBinding.inflate(inflater, card.coursesContainer, false)
                row.tvCourseName.text = if (course.name.isNotBlank()) course.name else venue.name
                row.tvHoleCount.text = getString(R.string.hole_count, course.holes ?: 9)
                row.divider.isGone = idx == venue.courses.lastIndex
                row.rowCourse.setOnClickListener {
                    prefillWizard(venue.name, course.name, course.holes ?: 9)
                }
                card.coursesContainer.addView(row.root)
            }
            b.nearbyContainer.addView(card.root)
        }
    }

    private fun prefillWizard(venueName: String, courseName: String, holes: Int) {
        findNavController().currentBackStackEntry?.savedStateHandle?.apply {
            set("prefillVenueName", venueName)
            set("prefillCourseName", if (courseName.isNotBlank()) courseName else "A코스")
            set("prefillHoles", holes)
        }
        findNavController().navigate(R.id.courseWizardFragment)
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
