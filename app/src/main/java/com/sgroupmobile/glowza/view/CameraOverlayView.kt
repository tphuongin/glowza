package com.sgroupmobile.glowza.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CameraOverlayView(context: Context, attr: AttributeSet): View(context, attr) {
    var isGridOn = true
    private val gridPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 1f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if(isGridOn){
            drawGrid(canvas)
        }
    }
    private fun drawGrid(canvas: Canvas){
        val x = (width / 3).toFloat()
        val y = (height / 3).toFloat()
        val lines = floatArrayOf(
            x, 0f, x, height.toFloat(),
            2 * x, 0f, 2 * x, height.toFloat(),
            0f, y, width.toFloat(), y,
            0f, 2 * y, height.toFloat(), 2 * y
        )
        canvas.drawLines(lines, gridPaint)
    }
}