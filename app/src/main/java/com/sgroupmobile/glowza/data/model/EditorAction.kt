package com.sgroupmobile.glowza.data.model

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

sealed class EditorAction {

    class Crop(val rectF: RectF): EditorAction()

    class Text(
        val text: String,
        val matrix: Matrix,
        val textPaint: Paint
    ): EditorAction()

    class Sticker(
        val sticker: Bitmap,
        val matrix: Matrix
    ): EditorAction()

    class Frame( val frame: Bitmap): EditorAction()

    class Filter(val colorMatrix: ColorMatrix): EditorAction()

    class Draw(
        val path: Path,
        val drawPaint: Paint
    ): EditorAction()

    data class Adjustment(
        val brightness: Float,
        val contrast: Float,
        val saturation: Float
    ) : EditorAction()
}