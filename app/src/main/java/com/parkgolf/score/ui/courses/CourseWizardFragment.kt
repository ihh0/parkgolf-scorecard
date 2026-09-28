package com.parkgolf.score.ui.courses

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.databinding.FragmentCourseWizardBinding
import com.parkgolf.score.ui.common.confirmYesNo
import com.parkgolf.score.ui.common.onBackPressed
import com.parkgolf.score.ui.start.SelectionHolder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CourseWizardFragment : Fragment(R.layout.fragment_course_wizard) {

    private val vm: CourseWizardViewModel by viewModels()
    private var venues: List<VenueEntity> = emptyList()
    private var existingCourseNames: List<String> = emptyList()
    private var savedVenueId = 0L
    private var savedCourseId = 0L
    private var savedVenueName = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentCourseWizardBinding.bind(view)
        val repo = App.repo(requireActivity().application)

        val holeAdapter = HoleCardAdapter(
            onPar = { i, v -> vm.setPar(i, v) },
            onDelete = { i -> vm.deleteHole(i) },
        )
        binding.stepHoles.rvHoles.layoutManager = LinearLayoutManager(requireContext())
        binding.stepHoles.rvHoles.adapter = holeAdapter

        val suggestAdapter = VenueSuggestionAdapter { picked -> vm.setVenueName(picked) }
        binding.stepVenue.rvSuggestions.layoutManager = LinearLayoutManager(requireContext())
        binding.stepVenue.rvSuggestions.adapter = suggestAdapter

        val previewAdapter = ParPreviewAdapter()
        binding.stepSave.rvParPreview.layoutManager = GridLayoutManager(requireContext(), 6)
        binding.stepSave.rvParPreview.adapter = previewAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeVenues().collect { venues = it; renderStep1(binding, suggestAdapter) }
        }

        if (savedInstanceState == null) {
            val handle = findNavController().previousBackStackEntry?.savedStateHandle
            val prefillVenue = handle?.get<String>("prefillVenueName")
            if (prefillVenue != null) {
                vm.initPrefill(
                    prefillVenue,
                    handle.get<String>("prefillCourseName") ?: "",
                    handle.get<Int>("prefillHoles") ?: 9,
                )
            } else {
                val venueId = handle?.get<Long>("wizardVenueId") ?: 0L
                val courseId = handle?.get<Long>("wizardCourseId") ?: 0L
                if (venueId == 0L || courseId == 0L) vm.initNew("")
                else viewLifecycleOwner.lifecycleScope.launch {
                    val venue = repo.observeVenues().first().firstOrNull { it.id == venueId }
                    val course = repo.coursesForVenue(venueId).firstOrNull { it.id == courseId }
                    if (venue != null && course != null)
                        vm.loadForEdit(venue.id, course.id, venue.name, course.name, course.pars)
                    else vm.initNew("")
                }
            }
        }

        var settingText = false
        binding.stepVenue.etVenueName.doAfterTextChanged {
            if (!settingText) vm.setVenueName(it?.toString() ?: "")
        }
        binding.stepCourse.etCourseName.doAfterTextChanged {
            if (!settingText) vm.setCourseName(it?.toString() ?: "")
        }
        binding.stepHoles.btnAddHole.setOnClickListener { vm.addHole() }

        binding.btnPrev.setOnClickListener {
            if (vm.draft.value?.step == 0) attemptExit() else vm.prev()
        }
        binding.btnNext.setOnClickListener { onNext(binding, repo) }

        binding.step0.setOnClickListener { vm.goToStep(0) }
        binding.step1.setOnClickListener { vm.goToStep(1) }
        binding.step2.setOnClickListener { vm.goToStep(2) }
        binding.step3.setOnClickListener { vm.goToStep(3) }

        binding.topBar.btnBack.setOnClickListener {
            if (vm.draft.value?.step == 0) attemptExit() else vm.prev()
        }
        onBackPressed { if (vm.draft.value?.step == 0) attemptExit() else vm.prev() }

        binding.stepSuccess.btnStartGame.setOnClickListener {
            SelectionHolder.reset()
            SelectionHolder.venueId = savedVenueId
            SelectionHolder.venueName = savedVenueName
            SelectionHolder.chosenCourseIds.add(savedCourseId)
            findNavController().navigate(R.id.playerSetupFragment)
        }
        binding.stepSuccess.btnToList.setOnClickListener { findNavController().popBackStack() }

        vm.draft.observe(viewLifecycleOwner) { draft ->
            settingText = true
            if (binding.stepVenue.etVenueName.text.toString() != draft.venueName)
                binding.stepVenue.etVenueName.setText(draft.venueName)
            if (binding.stepCourse.etCourseName.text.toString() != draft.courseName)
                binding.stepCourse.etCourseName.setText(draft.courseName)
            settingText = false

            binding.topBar.tvBarTitle.text = getString(
                if (vm.isEditing()) R.string.wizard_title_edit else R.string.wizard_full_title
            )
            binding.flipper.displayedChild = draft.step
            styleSteps(binding, draft.step)

            refreshExistingCourses(repo, draft.venueName)

            renderStep1(binding, suggestAdapter)
            renderStep2(binding)
            renderStep3(binding, holeAdapter)
            renderStep4(binding, previewAdapter)

            binding.btnNext.text = when (draft.step) {
                CourseWizardLogic.MAX_STEP -> getString(R.string.save_now)
                else -> getString(R.string.wizard_next)
            }
            binding.btnPrev.text =
                if (draft.step == 0) getString(R.string.cancel) else getString(R.string.wizard_prev)
            val enabled = isStepValid(draft.step, draft)
            binding.btnNext.isEnabled = enabled
            binding.btnNext.alpha = if (enabled) 1f else 0.3f
        }
    }

    private fun isStepValid(step: Int, draft: CourseDraft): Boolean = when (step) {
        0 -> CourseWizardLogic.venueStepValid(draft)
        1 -> CourseWizardLogic.courseStepValid(draft.courseName, existingCourseNames)
        2 -> CourseWizardLogic.holeStepValid(draft.pars)
        else -> true
    }

    private fun onNext(binding: FragmentCourseWizardBinding, repo: ParkGolfRepository) {
        val d = vm.draft.value ?: return
        if (!isStepValid(d.step, d)) return
        if (d.step == CourseWizardLogic.MAX_STEP) save(binding, repo)
        else vm.next()
    }

    private fun refreshExistingCourses(repo: ParkGolfRepository, venueName: String) {
        val vid = CourseWizardLogic.resolveVenueId(venueName, venues, vm.draft.value?.editingVenueId)
        if (vid == null) { existingCourseNames = emptyList(); return }
        viewLifecycleOwner.lifecycleScope.launch {
            val editingId = vm.draft.value?.editingCourseId
            existingCourseNames = repo.coursesForVenue(vid)
                .filter { it.id != editingId }.map { it.name }
        }
    }

    private fun renderStep1(binding: FragmentCourseWizardBinding, adapter: VenueSuggestionAdapter) {
        val name = vm.draft.value?.venueName ?: ""
        val existing = CourseWizardLogic.isExistingVenue(name, venues)
        val matches = CourseWizardLogic.matchedVenues(name, venues)
        binding.stepVenue.badgeExisting.isVisible = existing
        binding.stepVenue.bannerExisting.isVisible = existing
        val showSuggest = matches.isNotEmpty() && !existing
        binding.stepVenue.tvSuggestLabel.isVisible = showSuggest
        binding.stepVenue.rvSuggestions.isVisible = showSuggest
        adapter.submit(matches)
    }

    private fun renderStep2(binding: FragmentCourseWizardBinding) {
        val d = vm.draft.value ?: return
        binding.stepCourse.chipVenue.text = d.venueName
        val dup = CourseWizardLogic.isDuplicateCourseName(d.courseName, existingCourseNames)
        binding.stepCourse.errorDuplicate.isVisible = dup
        binding.stepCourse.etCourseName.setBackgroundResource(
            if (dup) R.drawable.bg_input_error else R.drawable.bg_input
        )
        binding.stepCourse.tvExistingLabel.isVisible = existingCourseNames.isNotEmpty()
        val flex = binding.stepCourse.flexExistingCourses
        flex.removeAllViews()
        existingCourseNames.forEach { c ->
            val tv = layoutInflater.inflate(R.layout.item_course_chip, flex, false) as android.widget.TextView
            tv.text = c
            flex.addView(tv)
        }
    }

    private fun renderStep3(binding: FragmentCourseWizardBinding, adapter: HoleCardAdapter) {
        val d = vm.draft.value ?: return
        binding.stepHoles.chipVenue.text = d.venueName
        binding.stepHoles.chipCourse.text = d.courseName
        binding.stepHoles.tvTotalHoles.text = getString(R.string.total_holes, d.pars.size)
        binding.stepHoles.tvTotalPar.text = d.pars.sum().toString()
        adapter.submit(d.pars)
    }

    private fun renderStep4(binding: FragmentCourseWizardBinding, adapter: ParPreviewAdapter) {
        val d = vm.draft.value ?: return
        binding.stepSave.tvSumVenue.text = getString(R.string.save_summary_venue, d.venueName)
        binding.stepSave.tvSumCourse.text = getString(R.string.save_summary_course, d.courseName, d.pars.size)
        binding.stepSave.tvSumHoles.text = getString(R.string.total_holes, d.pars.size)
        binding.stepSave.tvSumTotalPar.text = getString(R.string.total_par_value, d.pars.sum())
        adapter.submit(d.pars)
    }

    private fun styleSteps(binding: FragmentCourseWizardBinding, step: Int) {
        val icons = listOf(binding.stepIcon0, binding.stepIcon1, binding.stepIcon2, binding.stepIcon3)
        val checks = listOf(binding.stepCheck0, binding.stepCheck1, binding.stepCheck2, binding.stepCheck3)
        val labels = listOf(binding.stepLabel0, binding.stepLabel1, binding.stepLabel2, binding.stepLabel3)
        for (i in 0..3) {
            val done = step > i
            val active = step == i
            val circle = (icons[i].parent as View)
            circle.setBackgroundResource(
                when { done -> R.drawable.bg_step_done; active -> R.drawable.bg_step_active; else -> R.drawable.bg_step_inactive }
            )
            checks[i].isVisible = done
            icons[i].isVisible = !done
            val attr = if (active || done) R.attr.parkPrimary else R.attr.parkMutedForeground
            labels[i].setTextColor(MaterialColors.getColor(requireContext(), attr, 0))
        }
    }

    private fun attemptExit() {
        val titleRes = if (vm.isEditing()) R.string.confirm_cancel_edit else R.string.confirm_cancel_create
        confirmYesNo(titleRes) { findNavController().popBackStack() }
    }

    private fun showEditing(binding: FragmentCourseWizardBinding) {
        binding.flipper.displayedChild = 0
        binding.stepBar.visibility = View.VISIBLE
        binding.bottomBar.visibility = View.VISIBLE
    }

    private fun save(binding: FragmentCourseWizardBinding, repo: ParkGolfRepository) {
        val draft = vm.draft.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val allVenues = repo.observeVenues().first()
            val resolved = CourseWizardLogic.resolveVenueId(draft.venueName, allVenues, draft.editingVenueId)
            val venueId = resolved ?: repo.upsertVenue(VenueEntity(name = draft.venueName.trim()))
            if (draft.editingVenueId != null)
                repo.upsertVenue(VenueEntity(id = venueId, name = draft.venueName.trim()))
            val courseId = repo.upsertCourse(
                CourseEntity(
                    id = draft.editingCourseId ?: 0L, venueId = venueId,
                    name = draft.courseName.trim(), pars = draft.pars,
                )
            )
            savedVenueId = venueId
            savedCourseId = courseId
            savedVenueName = draft.venueName.trim()
            binding.stepSuccess.tvSavedInfo.text =
                getString(R.string.saved_info, draft.venueName, draft.courseName)
            binding.flipper.displayedChild = 4
            binding.stepBar.visibility = View.GONE
            binding.bottomBar.visibility = View.GONE
        }
    }
}
