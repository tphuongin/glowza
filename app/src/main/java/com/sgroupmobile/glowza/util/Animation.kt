package com.sgroupmobile.glowza.util

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
object CustomAnimation{
    fun toggleViewSmooth(view: View, isVisible: Boolean) {
        if (isVisible) {
            view.visibility = View.VISIBLE
            view.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        } else {
            view.animate()
                .alpha(0f)
                .translationX(20f)
                .setDuration(250)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { view.visibility = View.GONE }
                .start()
        }
    }
}

