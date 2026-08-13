package com.parkgolf.score.ui.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentPlayerSetupBinding
import com.parkgolf.score.databinding.ViewPlayerInputRowBinding
import com.parkgolf.score.domain.RoundFactory
import com.parkgolf.score.ui.RoundSessionViewModel
import kotlinx.coroutines.launch

class PlayerSetupFragment : Fragment(R.layout.fragment_player_setup) {
    private val session: RoundSessionViewModel by activityViewModels()
    private val rows = mutableListOf<ViewPlayerInputRowBinding>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentPlayerSetupBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_player_setup)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)
        val defaultName = App.settings(requireActivity().application).defaultPlayerName

        binding.tvVenueName.text = SelectionHolder.venueName ?: ""
        viewLifecycleOwner.lifecycleScope.launch {
            val venueId = SelectionHolder.venueId
            val courseId = SelectionHolder.chosenCourseIds.firstOrNull()
            if (venueId != null && courseId != null) {
                val course = repo.coursesForVenue(venueId).firstOrNull { it.id == courseId }
                binding.tvCourseName.text = course?.name ?: ""
            }
        }

        val inflater = LayoutInflater.from(requireContext())
        fun refresh() {
            rows.forEachIndexed { i, row ->
                row.tvIndex.text = (i + 1).toString()
                row.btnDelete.isEnabled = rows.size > 1
                row.btnDelete.alpha = if (rows.size > 1) 1f else 0.2f
            }
            val canStart = rows.all { it.etName.text.toString().trim().isNotEmpty() }
            binding.btnStartPlay.isEnabled = canStart
            binding.btnStartPlay.alpha = if (canStart) 1f else 0.3f
        }
        fun addRow(initial: String, focus: Boolean) {
            val row = ViewPlayerInputRowBinding.inflate(inflater, binding.playersContainer, false)
            row.etName.setText(initial)
            row.etName.doAfterTextChanged { refresh() }
            row.btnDelete.setOnClickListener {
                if (rows.size <= 1) return@setOnClickListener
                binding.playersContainer.removeView(row.root)
                rows.remove(row)
                refresh()
            }
            rows.add(row)
            binding.playersContainer.addView(row.root)
            refresh()
            if (focus) { row.etName.requestFocus(); row.etName.selectAll() }
        }

        addRow(defaultName, focus = false)
        binding.btnAddPlayer.setOnClickListener {
            addRow(getString(R.string.player_default_name, rows.size + 1), focus = true)
        }

        binding.btnStartPlay.setOnClickListener {
            val venueId = SelectionHolder.venueId ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                val courses = repo.coursesForVenue(venueId)
                val coursePars = SelectionHolder.chosenCourseIds.mapNotNull { id ->
                    courses.firstOrNull { it.id == id }?.let { RoundFactory.CoursePars(it.name, it.pars) }
                }
                if (coursePars.isEmpty()) return@launch
                val players = rows.mapIndexed { i, row ->
                    val t = row.etName.text.toString().trim()
                    if (t.isNotEmpty()) t
                    else if (i == 0) defaultName else getString(R.string.companion_default, i)
                }
                val venueName = SelectionHolder.venueName ?: ""
                val round = RoundFactory.newRound(venueName, players, coursePars, System.currentTimeMillis())
                val id = repo.saveRound(round)
                session.startRound(round.copy(id = id))
                findNavController().navigate(R.id.holeInputFragment)
            }
        }
    }
}
