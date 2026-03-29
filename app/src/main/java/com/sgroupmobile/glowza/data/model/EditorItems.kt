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

    // Trong class TextItem
    override fun getMappedPoints(): FloatArray {
        val w = getWidth()
        val h = getHeight()
        val p = 5f

        boundPoints[0] = -p;      boundPoints[1] = -p    // Top-Left
        boundPoints[2] = w + p;   boundPoints[3] = -p    // Top-Right
        boundPoints[4] = w + p;   boundPoints[5] = h + p // Bottom-Right
        boundPoints[6] = -p;      boundPoints[7] = h + p // Bottom-Left

        matrix.mapPoints(_mappedPoints, boundPoints)
        return _mappedPoints
    }
    fun updateBounds() {
        fontMetrics = textPaint.fontMetrics
    }
    fun updateText(newText: String) {
        this.text = newText
    }
    fun applyStyleFrom(other: TextItem) {
        this.textPaint.set(other.textPaint)
        this.setTextAlpha(other.textPaint.alpha)
        updateBounds()
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
