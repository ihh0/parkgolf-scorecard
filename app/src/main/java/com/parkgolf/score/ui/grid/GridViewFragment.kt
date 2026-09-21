package com.parkgolf.score.ui.grid

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentGridViewBinding
import com.parkgolf.score.ui.RoundSessionViewModel
import com.parkgolf.score.ui.common.ScoreTable

class GridViewFragment : Fragment(R.layout.fragment_grid_view) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentGridViewBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_grid)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val round = session.round.value ?: run { findNavController().popBackStack(); return }

        ScoreTable.render(binding.tableGrid, round) { h ->
            findNavController().previousBackStackEntry?.savedStateHandle?.set("jumpToHole", h)
            findNavController().popBackStack()
        }
    }
}
