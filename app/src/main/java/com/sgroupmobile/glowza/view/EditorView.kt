package com.sgroupmobile.glowza.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sgroupmobile.glowza.base.BaseItem
import com.sgroupmobile.glowza.ui.photo_editor.DrawItem

class EditorView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var baseBitmap: Bitmap? = null
    private val baseMatrix = Matrix()
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var isDrawMode = false
    private val drawPaths = mutableListOf<DrawItem>()
    private var currentPath: Path? = null
    private var currentDrawPaint: Paint? = null
    private var brushColor = Color.parseColor("#F48FB1")
    private var brushSize = 20f
    private var isEraserMode = false

    private val items = mutableListOf<BaseItem>()
    private var selectedItem: BaseItem? = null
    private var filterMatrix: ColorMatrix? = null
    private var brightness = 0f
    private var contrast = 1f
    private var saturation = 1f

    /**
     * CÁC HÀM CÔNG CỤ VẼ (FIX LỖI UNRESOLVED REFERENCE)
     */
    fun setDrawMode(enabled: Boolean) {
        this.isDrawMode = enabled
        this.selectedItem = null // Bỏ chọn vật thể khi đang vẽ
        invalidate()
    }

    fun setBrushConfig(color: Int, size: Float, isEraser: Boolean) {
        this.brushColor = color
        this.brushSize = size
        this.isEraserMode = isEraser
    }

    fun undoLastDraw() {
        if (drawPaths.isNotEmpty()) {
            drawPaths.removeAt(drawPaths.size - 1)
            invalidate()
        }
    }

    fun clearAllDraw() {
        drawPaths.clear()
        invalidate()
    }

    /**
     * CÁC HÀM CÔNG CỤ KHÁC
     */
    fun getSelectedItem(): BaseItem? = selectedItem

    fun setBaseBitmap(bitmap: Bitmap) {
        this.baseBitmap = bitmap
        post {
            // 1. Tính toán tỉ lệ để ảnh lấp đầy chiều ngang HOẶC chiều dọc (Fit)
            // Loại bỏ nhân 0.9f để ảnh to hết cỡ
            val scaleX = width.toFloat() / bitmap.width
            val scaleY = height.toFloat() / bitmap.height

            // Dùng coerceAtMost nếu muốn hiện toàn bộ ảnh (không mất góc)
            // Dùng coerceAtLeast nếu muốn ảnh lấp đầy màn hình (có thể mất góc nếu tỉ lệ khác nhau)
            val scale = scaleX.coerceAtMost(scaleY)

            baseMatrix.reset()
            baseMatrix.postScale(scale, scale)

            // 2. Căn giữa ảnh tuyệt đối
            val dx = (width - bitmap.width * scale) / 2f
            val dy = (height - bitmap.height * scale) / 2f
            baseMatrix.postTranslate(dx, dy)

            invalidate()
        }
    }

    fun setFilter(matrix: ColorMatrix?) { this.filterMatrix = matrix; updateBasePaintFilter(); invalidate() }
    fun setAdjustments(b: Float, c: Float, s: Float) { brightness = b; contrast = c; saturation = s; updateBasePaintFilter(); invalidate() }

    private fun updateBasePaintFilter() {
        val cm = ColorMatrix()
        filterMatrix?.let { cm.set(it) }
        val adj = ColorMatrix(floatArrayOf(contrast, 0f, 0f, 0f, brightness * 255f, 0f, contrast, 0f, 0f, brightness * 255f, 0f, 0f, contrast, 0f, brightness * 255f, 0f, 0f, 0f, 1f, 0f))
        cm.postConcat(adj)
        val sat = ColorMatrix(); sat.setSaturation(saturation); cm.postConcat(sat)
        basePaint.colorFilter = ColorMatrixColorFilter(cm)
    }

    fun addItem(item: BaseItem) {
        if (isDrawMode) return
        items.forEach { it.isSelected = false }; item.isSelected = true; selectedItem = item
        val scale = (width * 0.4f) / item.getWidth()
        item.matrix.postScale(scale, scale)
        item.matrix.postTranslate((width - item.getWidth() * scale) / 2f, (height - item.getHeight() * scale) / 2f)
        items.add(item); invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 1. Vẽ ảnh gốc
        baseBitmap?.let { canvas.drawBitmap(it, baseMatrix, basePaint) }

        // 2. Vẽ các nét vẽ đã lưu (Draw Paths)
        for (drawItem in drawPaths) {
            drawItem.draw(canvas, Paint())
        }
        // Vẽ nét đang vẽ dở
        currentPath?.let { path ->
            currentDrawPaint?.let { paint -> canvas.drawPath(path, paint) }
        }

        // 3. Vẽ Stickers/Text
        for (item in items) {
            item.draw(canvas, Paint(Paint.ANTI_ALIAS_FLAG))
            if (item.isSelected && !isDrawMode) {
                // Vẽ khung chọn (Code lược bỏ cho ngắn gọn, Phương giữ code cũ nhé)
            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isDrawMode) return handleDrawTouch(event)

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedItem = items.findLast { it.containsPoint(x, y) }
                items.forEach { it.isSelected = (it == selectedItem) }
                selectedItem?.let { items.remove(it); items.add(it) }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                selectedItem?.let { item ->
                    // Logic di chuyển Sticker/Text
                    invalidate()
                }
            }
        }
        return true
    }

    private fun handleDrawTouch(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path().apply { moveTo(x, y) }
                currentDrawPaint = Paint().apply {
                    color = if (isEraserMode) Color.WHITE else brushColor // Demo Eraser bằng màu trắng hoặc Mode CLEAR
                    strokeWidth = brushSize
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    isAntiAlias = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath?.lineTo(x, y)
            }
            MotionEvent.ACTION_UP -> {
                currentPath?.let { path ->
                    currentDrawPaint?.let { paint -> drawPaths.add(DrawItem(path, paint)) }
                }
                currentPath = null
            }
        }
        invalidate()
        return true
    }
}