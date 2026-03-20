package com.sgroupmobile.glowza.data.model

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path

sealed class EditorAction(val priority: Long) {
    val timestamp: Long = System.currentTimeMillis()
    class Crop(
        val croppedBitmap: Bitmap?
    ): EditorAction(priority = 0)

    class Text(
        val text: String,
        val matrix: Matrix,
        val textPaint: Paint
    ): EditorAction(priority = 2)

    class Sticker(
        val sticker: Bitmap,
        val matrix: Matrix
    ): EditorAction(priority = 2)

    class Frame( val frame: Bitmap): EditorAction(priority = 3)

    class Filter(val colorMatrix: ColorMatrix): EditorAction(priority = 1)

    class Draw(
        val path: Path,
        val drawPaint: Paint
    ): EditorAction(priority = 2)

    data class Adjustment(
        val brightness: Float,
        val contrast: Float,
        val saturation: Float
    ) : EditorAction(priority = 1)
}