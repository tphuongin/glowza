package com.sgroupmobile.glowza.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import com.sgroupmobile.glowza.data.model.FaceFilter
import com.sgroupmobile.glowza.common.enum.FilterType

class CameraOverlayView(context: Context, attr: AttributeSet): View(context, attr) {
    var isGridOn = true
    var faces: List<Face> = emptyList()
    var imageSourceWidth = 0
    var imageSourceHeight = 0
    var isFrontCamera = true

    private var filterDrawable: Drawable? = null
    private var selectedFilter: FaceFilter? = null

    private val gridPaint = Paint().apply {
        color = Color.WHITE
        alpha = 100
        strokeWidth = 1f
    }

    fun setFilter(filter: FaceFilter?) {
        selectedFilter = filter
        filterDrawable = if (filter != null && filter.filterRes != -1) {
            ContextCompat.getDrawable(context, filter.filterRes)
        } else {
            null
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isGridOn) {
            drawGrid(canvas)
        }
        if (filterDrawable != null && faces.isNotEmpty()) {
            drawFaceFilters(canvas)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        val x = (width / 3).toFloat()
        val y = (height / 3).toFloat()
        val lines = floatArrayOf(
            x, 0f, x, height.toFloat(),
            2 * x, 0f, 2 * x, height.toFloat(),
            0f, y, width.toFloat(), y,
            0f, 2 * y, width.toFloat(), 2 * y
        )
        canvas.drawLines(lines, gridPaint)
    }

    private fun drawFaceFilters(canvas: Canvas) {
        val drawable = filterDrawable ?: return
        val filter = selectedFilter ?: return
        if (imageSourceWidth == 0 || imageSourceHeight == 0) return

        val scaleX = width.toFloat() / imageSourceWidth
        val scaleY = height.toFloat() / imageSourceHeight

        faces.forEach { face ->
            var cx: Float
            var cy: Float

            when (filter.type) {
                FilterType.EYES -> {
                    val left = face.getLandmark(FaceLandmark.LEFT_EYE)
                    val right = face.getLandmark(FaceLandmark.RIGHT_EYE)
                    if (left != null && right != null) {
                        cx = (left.position.x + right.position.x) / 2
                        cy = (left.position.y + right.position.y) / 2
                    } else {
                        cx = face.boundingBox.centerX().toFloat()
                        cy = face.boundingBox.centerY().toFloat()
                    }
                }
                FilterType.NOSE -> {
                    val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
                    cx = nose?.position?.x ?: face.boundingBox.centerX().toFloat()
                    cy = nose?.position?.y ?: face.boundingBox.centerY().toFloat()
                }
                FilterType.TOP_HEAD -> {
                    cx = face.boundingBox.centerX().toFloat()
                    cy = face.boundingBox.top.toFloat()
                }
            }

            // (Lật tọa độ X nếu là Camera trước)
            if (isFrontCamera) {
                cx = imageSourceWidth - cx
            }

            // Chuyển sang tọa độ View thực tế
            val viewCx = cx * scaleX
            val viewCy = cy * scaleY

            // tính toán kích thước
            val faceWidth = face.boundingBox.width() * scaleX
            val filterWidth = faceWidth * filter.scaleFactor
            val filterHeight = drawable.intrinsicHeight * (filterWidth / drawable.intrinsicWidth) // co giãn đúng tỉ lệ

            val finalCy = viewCy + (filterHeight * filter.offsetY)

            //Vẽ và Xoay (Rotate) theo độ nghiêng của đầu
            canvas.save()
            canvas.rotate(face.headEulerAngleZ, viewCx, finalCy)

            val left = (viewCx - filterWidth / 2).toInt()
            val top = (finalCy - filterHeight / 2).toInt()
            val right = (left + filterWidth).toInt()
            val bottom = (top + filterHeight).toInt()

            drawable.setBounds(left, top, right, bottom)
            drawable.draw(canvas)

            canvas.restore()
        }
    }
}