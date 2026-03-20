package com.sgroupmobile.glowza.util

import android.graphics.ColorMatrix
import com.sgroupmobile.glowza.data.model.ImageFilter


object FilterUtils {

    fun getOriginal(): ColorMatrix? = null

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
        return ColorMatrix(floatArrayOf(
            0.9f, 0.5f, 0.1f, 0f, 0f,
            0.3f, 0.8f, 0.1f, 0f, 0f,
            0.2f, 0.3f, 0.5f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    fun getCold(): ColorMatrix {
        return ColorMatrix(floatArrayOf(
            0.8f, 0f, 0f, 0f, 0f,
            0f, 0.9f, 0f, 0f, 0f,
            0f, 0f, 1.3f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    fun getWarm(): ColorMatrix {
        return ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 0.8f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    fun getPolaroid(): ColorMatrix {
        return ColorMatrix(floatArrayOf(
            1.438f, -0.062f, -0.062f, 0f, 0f,
            -0.122f, 1.378f, -0.122f, 0f, 0f,
            -0.016f, -0.016f, 1.483f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    fun getCinematic(): ColorMatrix {
        return ColorMatrix(floatArrayOf(
            0.9f, 0f, 0f, 0f, 0f,
            0f, 1.1f, 0f, 0f, 0f,
            0f, 0f, 1.2f, 0f, 0f,
            0.1f, 0.1f, 0.1f, 1f, 0f
        ))
    }

    fun getHighContrast(): ColorMatrix {
        val matrix = ColorMatrix()
        val contrast = 1.5f
        val translate = (-.5f * contrast + .5f) * 255f
        matrix.set(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        return matrix
    }


    fun getListImageFilter(): List<ImageFilter> {
        return listOf(
            ImageFilter(101, "Gốc", getOriginal()),
            ImageFilter(102, "Trắng đen", getGrayScale()),
            ImageFilter(103, "Cổ điển", getSepia()),
            ImageFilter(104, "Vintage", getVintage()),
            ImageFilter(105, "Lạnh", getCold()),
            ImageFilter(106, "Ấm áp", getWarm()),
            ImageFilter(107, "Polaroid", getPolaroid()),
            ImageFilter(108, "Điện ảnh", getCinematic()),
            ImageFilter(109, "Độ tương phản", getHighContrast())
        )
    }


    fun getMatrixById(id: Int): ColorMatrix? {
        return getListImageFilter().find { it.id == id }?.colorMatrix
    }
}