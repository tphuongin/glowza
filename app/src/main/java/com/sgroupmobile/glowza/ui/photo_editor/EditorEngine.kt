package com.sgroupmobile.glowza.ui.photo_editor

import android.graphics.*
import com.sgroupmobile.glowza.data.model.EditorAction
import androidx.core.graphics.withSave
import androidx.core.graphics.createBitmap

class EditorEngine(private val original: Bitmap) {

    fun render(actions: List<EditorAction>): Bitmap {
        if (actions.isEmpty()) return original.copy(Bitmap.Config.ARGB_8888, true)

        // 1. Sắp xếp: Priority thấp (0-Crop, 1-Filter) làm trước.
        // 2. Trong cùng priority (2-Sticker/Text), cái nào làm sau (timestamp lớn) đè lên cái cũ.
        val sortedActions = actions.sortedWith(
            compareBy<EditorAction> { it.priority }.thenBy { it.timestamp }
        )

        // Bắt đầu từ bản sao của ảnh gốc
        var resultBitmap = original.copy(Bitmap.Config.ARGB_8888, true)

        for (action in sortedActions) {
            when (action) {
                is EditorAction.Crop -> {
                    // Thay thế nền bằng ảnh đã crop.
                    // Priority 0 đảm bảo các Sticker cũ sẽ bị cắt theo bố cục mới.
                    resultBitmap = action.croppedBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                }

                is EditorAction.Filter -> {
                    // Áp dụng filter lên bitmap hiện tại
                    resultBitmap = applyFilter(resultBitmap, action.colorMatrix)
                }

                is EditorAction.Sticker -> {
                    val canvas = Canvas(resultBitmap)
                    canvas.drawBitmap(action.sticker, action.matrix, null)
                }

                is EditorAction.Text -> {
                    val canvas = Canvas(resultBitmap)
                    canvas.withSave {
                        concat(action.matrix)
                        drawText(action.text, 0f, 0f, action.textPaint)
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

                else -> {}
            }
        }
        return resultBitmap
    }

    private fun applyFilter(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val bitmap = createBitmap(source.width, source.height)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return bitmap
    }
}