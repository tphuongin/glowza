package com.sgroupmobile.glowza.util

import android.graphics.ColorMatrix

object FilterUtils {
    fun getOriginal() = null

    fun getGrayScale(): ColorMatrix {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        return matrix
    }

    fun getSepia(): ColorMatrix {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        val sepiaMatrix = ColorMatrix()
        sepiaMatrix.setScale(1f, 0.95f, 0.82f, 1f)
        matrix.postConcat(sepiaMatrix)
        return matrix
    }

    fun getVintage(): ColorMatrix {
        val matrix = ColorMatrix()
        matrix.set(floatArrayOf(
            0.9f, 0f, 0f, 0f, 0f,
            0f, 0.8f, 0f, 0f, 0f,
            0f, 0f, 0.5f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        return matrix
    }

    fun getCold(): ColorMatrix {
        val matrix = ColorMatrix()
        matrix.set(floatArrayOf(
            0.8f, 0f, 0f, 0f, 0f,
            0f, 0.9f, 0f, 0f, 0f,
            0f, 0f, 1.2f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        return matrix
    }
}