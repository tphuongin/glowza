package com.sgroupmobile.glowza.util

import android.view.View
import androidx.core.graphics.toColorInt

fun showFancySnackbar(message: String, view: View) {
    com.google.android.material.snackbar.Snackbar.make(view, message, 2000)
        .setBackgroundTint("#FFC1CC".toColorInt()) // Màu hồng Pastel
        .setTextColor(android.graphics.Color.WHITE)
        .show()
}