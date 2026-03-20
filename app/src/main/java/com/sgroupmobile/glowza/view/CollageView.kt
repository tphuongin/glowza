package com.sgroupmobile.glowza.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.sgroupmobile.glowza.data.model.CollageLayout

class CollageView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val images = mutableListOf<Bitmap>()
    private var layout: CollageLayout? = null

    fun setImages(bitmaps: List<Bitmap>) {
        images.clear()
        images.addAll(bitmaps)
        invalidate()
    }

    fun setLayout(layout: CollageLayout) {
        this.layout = layout
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val layout = layout ?: return
        if (images.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()

        layout.slots.forEachIndexed { index, rect ->
            if (index >= images.size) return@forEachIndexed

            val bitmap = images[index]

            val left = rect.left * width
            val top = rect.top * height
            val right = rect.right * width
            val bottom = rect.bottom * height

            val dst = RectF(left, top, right, bottom)

            canvas.drawBitmap(bitmap, null, dst, null)
        }
    }
}