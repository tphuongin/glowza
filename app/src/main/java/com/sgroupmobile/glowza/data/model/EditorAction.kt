package com.sgroupmobile.glowza.data.model

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.Matrix
import android.graphics.Paint

sealed class EditorAction(val priority: Int) {

    val timestamp: Long = System.currentTimeMillis()
    class Crop(
        val croppedBitmap: Bitmap?
    ): EditorAction(priority = 0)

    class Text(
        var text: String,
        val matrix: Matrix,
        val textPaint: Paint,
        var id: Long = System.currentTimeMillis(),
        val displayMatrix: Matrix
    ): EditorAction(priority = 2)

    class Sticker(
        val sticker: Bitmap,
        val matrix: Matrix,
        var id: Long = System.currentTimeMillis(),
        val displayMatrix: Matrix
    ): EditorAction(priority = 2)

    class Frame( val frame: Bitmap): EditorAction(priority = 3)

    class Filter(val colorMatrix: ColorMatrix): EditorAction(priority = 1)

    class Draw(
        val paths: List<DrawItem>,
        val displayMatrix: Matrix
    ): EditorAction(priority = 2)

    data class Adjustment(
        val brightness: Float,
        val contrast: Float,
        val saturation: Float
    ) : EditorAction(priority = 1)
}