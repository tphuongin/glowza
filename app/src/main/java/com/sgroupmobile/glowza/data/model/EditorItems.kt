package com.sgroupmobile.glowza.data.model


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import com.sgroupmobile.glowza.base.BaseItem
import androidx.core.graphics.withMatrix

// Lớp cho Sticker
class StickerItem(var bitmap: Bitmap) : BaseItem() {
    override fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawBitmap(bitmap, matrix, paint)
    }
    override fun getWidth() = bitmap.width.toFloat()
    override fun getHeight() = bitmap.height.toFloat()
}

// Lớp cho Text
class TextItem(
    var text: String,
    val textPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
) : BaseItem() {

    private val bounds = Rect()

    init {
        updateBounds()
    }

    fun updateBounds() {
        textPaint.getTextBounds(text, 0, text.length, bounds)
    }

    // Các hàm setter để thay đổi kiểu dáng từ Sub-tool
    fun setTextColor(color: Int) {
        textPaint.color = color
    }

    fun setTextAlpha(alpha: Int) { // 0-255
        textPaint.alpha = alpha
    }

    fun setTextSize(size: Float) {
        textPaint.textSize = size
        updateBounds()
    }

    fun setTypeface(typeface: Typeface) {
        textPaint.typeface = typeface
        updateBounds()
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        canvas.withMatrix(matrix) {
            // Vẽ từ tọa độ (0, chiều cao bounds) để chữ không bị mất phần chân
            drawText(text, 0f, bounds.height().toFloat(), textPaint)
        }
    }

    override fun getWidth() = textPaint.measureText(text)
    override fun getHeight() = bounds.height().toFloat() * 1.5f
}

class DrawItem(
    val path: Path,
    val paint: Paint
) : BaseItem() {

    // Vẽ nét vẽ lên Canvas
    override fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawPath(path, this.paint)
    }

    // Vì DrawItem phủ toàn màn hình nên ta trả về 0 để tránh scale nhầm
    override fun getWidth() = 0f
    override fun getHeight() = 0f
}