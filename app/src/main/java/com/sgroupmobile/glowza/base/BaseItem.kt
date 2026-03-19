package com.sgroupmobile.glowza.base

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint

abstract class BaseItem {
    val matrix = Matrix()
    var isSelected = false

    // Dùng dấu gạch dưới để tránh xung đột với tên hàm Getter
    protected val boundPoints = FloatArray(8)
    protected val _mappedPoints = FloatArray(8)

    abstract fun draw(canvas: Canvas, paint: Paint)
    abstract fun getWidth(): Float
    abstract fun getHeight(): Float

    // Ánh xạ tọa độ từ Matrix ra màn hình
    fun getMappedPoints(): FloatArray {
        val w = getWidth()
        val h = getHeight()

        // Cập nhật tọa độ gốc của item (hình chữ nhật 0,0 đến w,h)
        boundPoints[0] = 0f; boundPoints[1] = 0f
        boundPoints[2] = w;  boundPoints[3] = 0f
        boundPoints[4] = w;  boundPoints[5] = h
        boundPoints[6] = 0f; boundPoints[7] = h

        // Dùng Matrix để tính toán tọa độ thực tế trên màn hình (đã xoay, scale)
        matrix.mapPoints(_mappedPoints, boundPoints)
        return _mappedPoints
    }

    // Kiểm tra điểm chạm (x, y) có nằm trong vùng của Item không
    fun containsPoint(x: Float, y: Float): Boolean {
        val tempMatrix = Matrix()
        matrix.invert(tempMatrix)
        val pts = floatArrayOf(x, y)
        tempMatrix.mapPoints(pts)
        return pts[0] >= 0 && pts[0] <= getWidth() && pts[1] >= 0 && pts[1] <= getHeight()
    }
}