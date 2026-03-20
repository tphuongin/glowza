package com.sgroupmobile.glowza.ui.photo_editor

import android.graphics.*
import com.sgroupmobile.glowza.data.model.EditorAction
import androidx.core.graphics.withSave
import androidx.core.graphics.createBitmap

class EditorEngine(private val original: Bitmap) {

    fun render(actions: List<EditorAction>, isExporting: Boolean = false): Bitmap {
        if (actions.isEmpty()) return original.copy(Bitmap.Config.ARGB_8888, true)

        val sortedActions = actions.sortedWith(
            compareBy<EditorAction> { it.priority }.thenBy { it.timestamp }
        )

        var resultBitmap = original.copy(Bitmap.Config.ARGB_8888, true)

        for (action in sortedActions) {
            when (action) {
                is EditorAction.Crop -> {
                    resultBitmap = action.croppedBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                }

                is EditorAction.Filter -> {
                    resultBitmap = applyFilter(resultBitmap, action.colorMatrix)
                }

                is EditorAction.Adjustment -> {
                    resultBitmap = applyAdjustments(
                        resultBitmap,
                        action.brightness,
                        action.contrast,
                        action.saturation
                    )
                }

                is EditorAction.Sticker -> {
                    // chỉ vẽ khi lưu ảnh
                    if (isExporting) {
                        val canvas = Canvas(resultBitmap)
                        val invertMatrix = Matrix()
                        action.displayMatrix.invert(invertMatrix)

                        canvas.withSave {
                            concat(invertMatrix) // Đưa về hệ tọa độ ảnh gốc
                            concat(action.matrix) // Áp dụng vị trí của sticker
                            canvas.drawBitmap(action.sticker, 0f, 0f, null)
                        }
                    }
                }

                is EditorAction.Text -> {
                    // chỉ vẽ khi lưu ảnh
                    if (isExporting) {
                        val canvas = Canvas(resultBitmap)
                        val invertMatrix = Matrix()
                        action.displayMatrix.invert(invertMatrix)

                        canvas.withSave {
                            concat(invertMatrix)
                            concat(action.matrix)

                            // Căn chỉnh Baseline cho Text để không bị lệch lề
                            val fontMetrics = action.textPaint.fontMetrics
                            canvas.drawText(action.text, 0f, -fontMetrics.ascent, action.textPaint)
                        }
                    }
                }

                is EditorAction.Draw -> {
                    val canvas = Canvas(resultBitmap)
                    val invertMatrix = Matrix()
                    action.displayMatrix.invert(invertMatrix)

                    canvas.withSave {
                        concat(invertMatrix)

                        val layerRect = RectF(0f, 0f, resultBitmap.width.toFloat(), resultBitmap.height.toFloat())
                        val drawingLayer = canvas.saveLayer(layerRect, null)

                        for (drawItem in action.paths) {
                            canvas.drawPath(drawItem.path, drawItem.paint)
                        }

                        canvas.restoreToCount(drawingLayer)
                    }
                }

                is EditorAction.Frame -> {
                    val canvas = Canvas(resultBitmap)
                    val fullImageRect = Rect(0, 0, resultBitmap.width, resultBitmap.height)
                    canvas.drawBitmap(action.frame, null, fullImageRect, null)
                }
            }
        }
        return resultBitmap
    }

    private fun applyFilter(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val bitmap = createBitmap(source.width, source.height)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return bitmap
    }

    private fun applyAdjustments(source: Bitmap, b: Float, c: Float, s: Float): Bitmap {
        val bitmap = createBitmap(source.width, source.height)
        val canvas = Canvas(bitmap)

        val cm = ColorMatrix()
        val adj = ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, b * 255f,
            0f, c, 0f, 0f, b * 255f,
            0f, 0f, c, 0f, b * 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(adj)

        val sat = ColorMatrix()
        sat.setSaturation(s)
        cm.postConcat(sat)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }

        canvas.drawBitmap(source, 0f, 0f, paint)
        return bitmap
    }
}