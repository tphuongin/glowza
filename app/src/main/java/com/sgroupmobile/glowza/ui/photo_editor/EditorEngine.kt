package com.sgroupmobile.glowza.ui.photo_editor

import android.graphics.*
import androidx.core.graphics.withSave
import com.sgroupmobile.glowza.data.model.EditorAction

/**
 * EditorEngine: Trái tim của trình chỉnh sửa ảnh.
 * Chịu trách nhiệm render tất cả các lớp (layers) từ ảnh gốc theo thứ tự.
 */
class EditorEngine(private val original: Bitmap) {

    // Danh sách các thao tác để hỗ trợ Undo/Redo
    private val undoAction = mutableListOf<EditorAction>()
    private val redoAction = mutableListOf<EditorAction>()

    /**
     * Thêm một thao tác mới và render lại ảnh
     */
    fun addAction(action: EditorAction): Bitmap {
        undoAction.add(action)
        redoAction.clear()
        return render()
    }

    /**
     * Thực hiện lại thao tác vừa Undo
     */
    fun redo(): Bitmap {
        if (redoAction.isNotEmpty()) {
            undoAction.add(redoAction.removeAt(redoAction.size - 1))
        }
        return render()
    }

    /**
     * Quay lại thao tác trước đó
     */
    fun undo(): Bitmap {
        if (undoAction.isNotEmpty()) {
            redoAction.add(undoAction.removeAt(undoAction.size - 1))
        }
        return render()
    }

    /**
     * Render toàn bộ "Stack" các thao tác lên ảnh gốc
     */
    fun render(): Bitmap {
        // Luôn copy từ ảnh gốc để tránh chỉnh sửa đè (Destructive Editing)
        var resultBitmap = original.copy(Bitmap.Config.ARGB_8888, true)

        for (action in undoAction) {
            when (action) {
                is EditorAction.Crop -> {
                    val rect = action.rectF
                    // Tính toán tọa độ cắt an toàn
                    val left = rect.left.toInt().coerceIn(0, resultBitmap.width)
                    val top = rect.top.toInt().coerceIn(0, resultBitmap.height)
                    val width = rect.width().toInt().coerceAtMost(resultBitmap.width - left)
                    val height = rect.height().toInt().coerceAtMost(resultBitmap.height - top)

                    if (width > 0 && height > 0) {
                        val cropped = Bitmap.createBitmap(resultBitmap, left, top, width, height)
                        // Giải phóng bộ nhớ bitmap cũ nếu không phải ảnh gốc ban đầu
                        if (resultBitmap != original) resultBitmap.recycle()
                        resultBitmap = cropped
                    }
                }

                is EditorAction.Frame -> {
                    val canvas = Canvas(resultBitmap)
                    val destRect = Rect(0, 0, resultBitmap.width, resultBitmap.height)
                    canvas.drawBitmap(action.frame, null, destRect, null)
                }

                is EditorAction.Sticker -> {
                    val canvas = Canvas(resultBitmap)
                    canvas.drawBitmap(action.sticker, action.matrix, null)
                }

                is EditorAction.Draw -> {
                    val canvas = Canvas(resultBitmap)
                    canvas.drawPath(action.path, action.drawPaint)
                }

                is EditorAction.Text -> {
                    val canvas = Canvas(resultBitmap)
                    canvas.withSave {
                        concat(action.matrix)
                        // Vẽ text với Paint của người dùng
                        drawText(action.text, 0f, 0f, action.textPaint)
                    }
                }
                else -> {}
            }
        }
        return resultBitmap
    }

    /**
     * Giải phóng tài nguyên khi không sử dụng nữa
     */
    fun clear() {
        undoAction.clear()
        redoAction.clear()
    }
}