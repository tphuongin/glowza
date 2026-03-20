package com.sgroupmobile.glowza.ui.photo_editor

import android.graphics.*
import com.sgroupmobile.glowza.data.model.EditorAction
import androidx.core.graphics.withSave
import androidx.core.graphics.createBitmap

class EditorEngine(private val original: Bitmap) {

    fun render(actions: List<EditorAction>): Bitmap {
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

                // --- BỔ SUNG: XỬ LÝ ADJUSTMENT ---
                is EditorAction.Adjustment -> {
                    resultBitmap = applyAdjustments(
                        resultBitmap,
                        action.brightness,
                        action.contrast,
                        action.saturation
                    )
                }

                is EditorAction.Sticker -> {
//                    val canvas = Canvas(resultBitmap)
//                    // Vẽ sticker bằng Matrix chính xác mà người dùng đã kéo ở EditorView
//                    canvas.drawBitmap(action.sticker, action.matrix, null)
                }

                is EditorAction.Text -> {
                    val canvas = Canvas(resultBitmap)
                    canvas.withSave {
                        concat(action.matrix)
                        // Giả sử textPaint đã được setup màu sắc và kích thước
                        canvas.drawText(action.text, 0f, 0f, action.textPaint)
                    }
                }

                is EditorAction.Draw -> {
                    val canvas = Canvas(resultBitmap)
                    canvas.drawPath(action.path, action.drawPaint)
                }

                is EditorAction.Frame -> {
                    val canvas = Canvas(resultBitmap)
                    val destRect = Rect(0, 0, resultBitmap.width, resultBitmap.height)
                    canvas.drawBitmap(action.frame, null, destRect, null)
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

    /**
     * Hàm xử lý Adjustment: Tính toán ma trận màu tổng hợp
     */
    private fun applyAdjustments(source: Bitmap, b: Float, c: Float, s: Float): Bitmap {
        val bitmap = createBitmap(source.width, source.height)
        val canvas = Canvas(bitmap)

        // 1. Tạo ma trận tổng hợp
        val cm = ColorMatrix()

        // Áp dụng Contrast và Brightness
        // Công thức: Color = Contrast * Color + Brightness
        val adj = ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, b * 255f,
            0f, c, 0f, 0f, b * 255f,
            0f, 0f, c, 0f, b * 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(adj)

        // Áp dụng Saturation
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