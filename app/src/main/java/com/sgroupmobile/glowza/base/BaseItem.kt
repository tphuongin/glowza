package com.sgroupmobile.glowza.base

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF

abstract class BaseItem {
    var matrix = Matrix()
    var isSelected = false

    var id: Long = 1
    protected val boundPoints = FloatArray(8)
    protected val _mappedPoints = FloatArray(8)

    abstract fun draw(canvas: Canvas, paint: Paint)
    abstract fun getWidth(): Float
    abstract fun getHeight(): Float

    // Trả về tọa độ 4 góc sau khi đã áp dụng Matrix (Xoay, Scale, Di chuyển)
    fun getMappedPoints(): FloatArray {
        val w = getWidth()
        val h = getHeight()

        // 0,1: Top-Left | 2,3: Top-Right | 4,5: Bottom-Right | 6,7: Bottom-Left
        boundPoints[0] = 0f; boundPoints[1] = 0f
        boundPoints[2] = w;  boundPoints[3] = 0f
        boundPoints[4] = w;  boundPoints[5] = h
        boundPoints[6] = 0f; boundPoints[7] = h

        matrix.mapPoints(_mappedPoints, boundPoints)
        return _mappedPoints
    }

    fun containsPoint(x: Float, y: Float): Boolean {
        val tempMatrix = Matrix()
        matrix.invert(tempMatrix)
        val pts = floatArrayOf(x, y)
        tempMatrix.mapPoints(pts)
        return pts[0] >= 0 && pts[0] <= getWidth() && pts[1] >= 0 && pts[1] <= getHeight()
    }
}