package com.parkgolf.score.ui.courses

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.databinding.FragmentCourseWizardBinding
import com.parkgolf.score.ui.common.confirmYesNo
import com.parkgolf.score.ui.common.onBackPressed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CourseWizardFragment : Fragment(R.layout.fragment_course_wizard) {

    private val vm: CourseWizardViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentCourseWizardBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        val parAdapter = WizardParAdapter { index, newValue -> vm.setPar(index, newValue) }
        binding.rvPars.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPars.adapter = parAdapter

        // Initial state: create vs edit (only when first created).
        if (savedInstanceState == null) {
            val handle = findNavController().previousBackStackEntry?.savedStateHandle
            val venueId = handle?.get<Long>("wizardVenueId") ?: 0L
            val courseId = handle?.get<Long>("wizardCourseId") ?: 0L
            if (venueId == 0L || courseId == 0L) {
                vm.initNew("A코스")
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    val venue = repo.observeVenues().first().firstOrNull { it.id == venueId }
                    val course = repo.coursesForVenue(venueId).firstOrNull { it.id == courseId }
                    if (venue != null && course != null) {
                        vm.loadForEdit(venue.id, course.id, venue.name, course.name, course.pars)
                    } else vm.initNew("A코스")
                }
            }
        }

        // Text inputs -> draft. guard prevents feedback loops when we set text from state.
        var binding_updating = false
        binding.etVenueName.doAfterTextChanged { if (!binding_updating) vm.setVenueName(it?.toString() ?: "") }
        binding.etCourseName.doAfterTextChanged { if (!binding_updating) vm.setCourseName(it?.toString() ?: "") }

        binding.btnHoleMinus.setOnClickListener { vm.setHoleCount((vm.draft.value?.holeCount ?: 9) - 1) }
        binding.btnHolePlus.setOnClickListener { vm.setHoleCount((vm.draft.value?.holeCount ?: 9) + 1) }

        binding.btnPrev.setOnClickListener {
            if (vm.draft.value?.step == 0) attemptExit(repo) else vm.prev()
        }
        binding.btnNext.setOnClickListener {
            val d = vm.draft.value ?: return@setOnClickListener
            if (!vm.canAdvance()) return@setOnClickListener
            if (d.step == CourseWizardLogic.MAX_STEP) save(repo, binding) else {
                if (d.step == 0 && !vm.isEditing()) maybeSuggestCourseName(repo)
                vm.next()
            }
        }

        // Step indicator taps jump to a step.
        binding.steps.step0.setOnClickListener { vm.goToStep(0) }
        binding.steps.step1.setOnClickListener { vm.goToStep(1) }
        binding.steps.step2.setOnClickListener { vm.goToStep(2) }
        binding.steps.step3.setOnClickListener { vm.goToStep(3) }

        // Top bar + system back both route to attemptExit at step 0, else previous step.
        binding.topBar.btnBack.setOnClickListener {
            if (vm.draft.value?.step == 0) attemptExit(repo) else vm.prev()
        }
        onBackPressed { if (vm.draft.value?.step == 0) attemptExit(repo) else vm.prev() }

        vm.draft.observe(viewLifecycleOwner) { draft ->
            binding_updating = true
            if (binding.etVenueName.text.toString() != draft.venueName)
                binding.etVenueName.setText(draft.venueName)
            if (binding.etCourseName.text.toString() != draft.courseName)
                binding.etCourseName.setText(draft.courseName)
            binding_updating = false

            binding.topBar.tvBarTitle.text =
                getString(if (vm.isEditing()) R.string.wizard_title_edit else R.string.wizard_title_create)
            binding.flipper.displayedChild = draft.step
            styleSteps(binding, draft.step)

            binding.tvHoleCount.text = draft.holeCount.toString()
            parAdapter.submit(draft.pars)

            binding.tvSummaryVenue.text = getString(R.string.save_summary_venue, draft.venueName)
            binding.tvSummaryCourse.text =
                getString(R.string.save_summary_course, draft.courseName, draft.holeCount)

            binding.btnPrev.text =
                if (draft.step == 0) getString(R.string.cancel) else getString(R.string.wizard_prev)
            binding.btnNext.text =
                if (draft.step == CourseWizardLogic.MAX_STEP) getString(R.string.wizard_save)
                else getString(R.string.wizard_next)
            val canAdvance = CourseWizardLogic.isStepValid(draft, draft.step)
            binding.btnNext.isEnabled = canAdvance
            binding.btnNext.alpha = if (canAdvance) 1f else 0.5f
        }
    }

    private fun styleSteps(binding: FragmentCourseWizardBinding, step: Int) {
        val labels = listOf(
            binding.steps.tvStep0Label, binding.steps.tvStep1Label,
            binding.steps.tvStep2Label, binding.steps.tvStep3Label,
        )
        labels.forEachIndexed { i, tv ->
            val active = i == step
            val colorRes = if (active) R.color.green_primary else R.color.text_muted
            tv.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
            tv.setTypeface(null, if (active) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }

    private fun maybeSuggestCourseName(repo: ParkGolfRepository) {
        val name = vm.draft.value?.venueName?.trim() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val venues = repo.observeVenues().first()
            val existingId = CourseWizardLogic.resolveVenueId(name, venues, null)
            if (existingId != null) {
                val names = repo.coursesForVenue(existingId).map { it.name }
                vm.setCourseName(CourseWizardLogic.suggestCourseName(names))
            }
        }
    }

    private fun attemptExit(@Suppress("UNUSED_PARAMETER") repo: ParkGolfRepository) {
        val titleRes =
            if (vm.isEditing()) R.string.confirm_cancel_edit else R.string.confirm_cancel_create
        confirmYesNo(titleRes) { findNavController().popBackStack() }
    }

    private fun save(repo: ParkGolfRepository, binding: FragmentCourseWizardBinding) {
        val draft = vm.draft.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val venues = repo.observeVenues().first()
            val resolved = CourseWizardLogic.resolveVenueId(draft.venueName, venues, draft.editingVenueId)
            val venueId = resolved ?: repo.upsertVenue(VenueEntity(name = draft.venueName.trim()))
            if (draft.editingVenueId != null) {
                repo.upsertVenue(VenueEntity(id = venueId, name = draft.venueName.trim()))
            }
            repo.upsertCourse(
                CourseEntity(
                    id = draft.editingCourseId ?: 0L,
                    venueId = venueId,
                    name = draft.courseName.trim(),
                    pars = draft.pars,
                )
            )
            Snackbar.make(binding.root, R.string.course_saved, Snackbar.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }
}
