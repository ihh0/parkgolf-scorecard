package com.parkgolf.score.ui.common

import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.parkgolf.score.R

/** Register a system/gesture back handler bound to this fragment's view lifecycle. */
fun Fragment.onBackPressed(handler: () -> Unit) {
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = handler()
    }
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
}

/** Show a 네/아니오 confirmation dialog; run [onYes] only if the user taps 네. */
fun Fragment.confirmYesNo(@StringRes titleRes: Int, onYes: () -> Unit) {
    AlertDialog.Builder(requireContext())
        .setTitle(titleRes)
        .setPositiveButton(R.string.yes) { _, _ -> onYes() }
        .setNegativeButton(R.string.no, null)
        .show()
}
