package com.parkgolf.score.ui.start

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentPlayerSetupBinding
import com.parkgolf.score.domain.RoundFactory
import com.parkgolf.score.ui.RoundSessionViewModel
import kotlinx.coroutines.launch

class PlayerSetupFragment : Fragment(R.layout.fragment_player_setup) {
    private val session: RoundSessionViewModel by activityViewModels()
    private val editTexts = mutableListOf<EditText>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentPlayerSetupBinding.bind(view)
        val repo = App.repo(requireActivity().application)

        fun addPlayerRow(initial: String) {
            if (editTexts.size >= 4) return
            val et = EditText(requireContext()).apply {
                setText(initial)
                textSize = 18f
                minHeight = resources.getDimensionPixelSize(R.dimen.touch_min)
            }
            editTexts.add(et)
            binding.playersContainer.addView(et)
            binding.btnAddPlayer.isEnabled = editTexts.size < 4
        }
        addPlayerRow(getString(R.string.me))
        binding.btnAddPlayer.setOnClickListener { addPlayerRow("") }

        binding.btnStartPlay.setOnClickListener {
            val venueId = SelectionHolder.venueId ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                val courses = repo.coursesForVenue(venueId)
                val coursePars = SelectionHolder.chosenCourseIds.mapNotNull { id ->
                    courses.firstOrNull { it.id == id }?.let { RoundFactory.CoursePars(it.name, it.pars) }
                }
                if (coursePars.isEmpty()) return@launch
                val players = editTexts.mapIndexed { i, et ->
                    val t = et.text.toString().trim()
                    if (t.isNotEmpty()) t
                    else if (i == 0) getString(R.string.me) else getString(R.string.companion_default, i)
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
