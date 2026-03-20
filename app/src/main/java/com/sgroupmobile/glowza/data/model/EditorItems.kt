package com.sgroupmobile.glowza.data.model


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
    private var fontMetrics: Paint.FontMetrics = textPaint.fontMetrics

    override fun getMappedPoints(): FloatArray {
        val w = getWidth()
        val h = getHeight()

        // 0,1: Top-Left | 2,3: Top-Right | 4,5: Bottom-Right | 6,7: Bottom-Left
        boundPoints[0] = 0f; boundPoints[1] = -h-10
        boundPoints[2] = w;  boundPoints[3] = -h-10
        boundPoints[4] = w;  boundPoints[5] = h
        boundPoints[6] = 0f; boundPoints[7] = h

        matrix.mapPoints(_mappedPoints, boundPoints)
        return _mappedPoints
    }
    fun updateBounds() {
        fontMetrics = textPaint.fontMetrics
    }
    fun updateText(newText: String) {
        this.text = newText
    }
    // Trong class TextItem.kt
    fun applyStyleFrom(other: TextItem) {
        this.textPaint.set(other.textPaint)
        this.setTextAlpha(other.textPaint.alpha)
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
            // Vẽ chữ bắt đầu từ tọa độ y = -ascent để chữ nằm gọn trong khung (0,0)
            drawText(text, 0f, -fontMetrics.ascent, textPaint)
        }
    }

    override fun getWidth() = textPaint.measureText(text) * 1.05f

    override fun getHeight(): Float {
        return (fontMetrics.descent - fontMetrics.ascent + fontMetrics.leading)
    }
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
