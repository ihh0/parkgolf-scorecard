package com.parkgolf.score.ui.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentSettingsBinding.bind(view)
        val store = App.settings(requireActivity().application)

        binding.topBar.tvBarTitle.text = getString(R.string.title_settings)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }

        val rows = mapOf(
            ThemeKey.GREEN to Pair(binding.rowGreen, binding.checkGreen),
            ThemeKey.BLUE to Pair(binding.rowBlue, binding.checkBlue),
            ThemeKey.ORANGE to Pair(binding.rowOrange, binding.checkOrange),
            ThemeKey.PURPLE to Pair(binding.rowPurple, binding.checkPurple),
        )
        val current = store.themeKey
        rows.forEach { (key, pair) ->
            val (row, check) = pair
            val selected = key == current
            check.isVisible = selected
            row.setBackgroundResource(
                if (selected) R.drawable.bg_theme_selected else R.drawable.bg_card
            )
            row.setOnClickListener {
                if (store.themeKey != key) {
                    store.themeKey = key
                    requireActivity().recreate()
                }
            }
        }

        binding.etDefaultName.setText(store.defaultPlayerName)
        binding.etDefaultName.doAfterTextChanged { text ->
            val name = text?.toString()?.trim().orEmpty()
            store.defaultPlayerName = if (name.isEmpty()) SettingsStore.DEFAULT_NAME else name
        }
    }
}
