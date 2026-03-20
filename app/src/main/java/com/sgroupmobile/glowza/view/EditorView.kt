package com.sgroupmobile.glowza.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseItem
import com.sgroupmobile.glowza.data.model.DrawItem
import kotlin.math.atan2
import kotlin.math.hypot

class EditorView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // Các hằng số chế độ chạm
    companion object {
        private const val MODE_NONE = 0
        private const val MODE_DRAG = 1
        private const val MODE_RESIZE_ROTATE = 2
        private const val HANDLE_RADIUS = 40f // Độ nhạy khi chạm vào nút chức năng
    }

    // Thành phần ảnh nền
    private var baseBitmap: Bitmap? = null
    private val baseMatrix = Matrix()
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Chế độ vẽ (Drawing Mode)
    private var isDrawMode = false
    private val drawPaths = mutableListOf<DrawItem>()
    private var currentPath: Path? = null
    private var currentDrawPaint: Paint? = null
    private var brushColor = Color.parseColor("#F48FB1")
    private var brushSize = 20f
    private var isEraserMode = false


    // Paint để vẽ Bitmap mượt hơn
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val iconSize = 50f // Kích thước hiển thị của icon (đường kính

    // Quản lý vật thể (Stickers/Text)
    private var items = mutableListOf<BaseItem>()
    private var selectedItem: BaseItem? = null
    private var touchMode = MODE_NONE
    private var lastX = 0f
    private var lastY = 0f

    // Paint cho khung viền và nút điều khiển
    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    // Bộ lọc và Điều chỉnh (Filter & Adjustments)
    private var filterMatrix: ColorMatrix? = null
    private var brightness = 0f
    private var contrast = 1f
    private var saturation = 1f
    private val deleteIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_remove)
    private val resizeIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_rotate)

        /**
     * Cấu hình chế độ vẽ
     */
    fun setDrawMode(enabled: Boolean) {
        this.isDrawMode = enabled
        if (enabled) {
            // Bỏ chọn tất cả vật thể khi chuyển sang chế độ vẽ
            items.forEach { it.isSelected = false }
            selectedItem = null
        }
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
     * Cấu hình ảnh nền
     */
    fun setBaseBitmap(bitmap: Bitmap) {
        this.baseBitmap = bitmap
        post {
            val scaleX = width.toFloat() / bitmap.width
            val scaleY = height.toFloat() / bitmap.height
            val scale = scaleX.coerceAtMost(scaleY)

            baseMatrix.reset()
            baseMatrix.postScale(scale, scale)
            val dx = (width - bitmap.width * scale) / 2f
            val dy = (height - bitmap.height * scale) / 2f
            baseMatrix.postTranslate(dx, dy)
            invalidate()
        }
    }
    fun updateItems(newList: List<BaseItem>) {
        this.items.clear()
        this.items.addAll(newList)

        // Hàm thực hiện căn chỉnh
        val setupMatrix = {
            items.forEach { item ->
                if (item.matrix.isIdentity) {
                    val scale = (width * 0.4f) / item.getWidth()
                    item.matrix.postScale(scale, scale)
                    val dx = (width - item.getWidth() * scale) / 2f
                    val dy = (height - item.getHeight() * scale) / 2f
                    item.matrix.postTranslate(dx, dy)
                }
            }
            invalidate() // Chỉ vẽ sau khi đã setup xong Matrix
        }

        // Nếu View đã đo đạc xong (width > 0), tính toán luôn để không bị giật
        if (width > 0 && height > 0) {
            setupMatrix()
        } else {
            // Nếu chưa (lúc mới mở App), mới dùng post
            post { setupMatrix() }
        }
    }
    /**
     * Thêm vật thể mới (Sticker/Text)
     */
    fun addItem(item: BaseItem) {
        if (isDrawMode) return

        items.forEach { it.isSelected = false }
        item.isSelected = true
        selectedItem = item

        // Tỉ lệ ban đầu và vị trí giữa màn hình
        val scale = (width * 0.4f) / item.getWidth()
        item.matrix.postScale(scale, scale)
        item.matrix.postTranslate(
            (width - item.getWidth() * scale) / 2f,
            (height - item.getHeight() * scale) / 2f
        )

        items.add(item)
        invalidate()
    }

    fun getSelectedItem(): BaseItem? = selectedItem

    /**
     * Xử lý Render
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Vẽ ảnh gốc với bộ lọc
        baseBitmap?.let { canvas.drawBitmap(it, baseMatrix, basePaint) }

        // 2. Vẽ các nét vẽ đã lưu
        for (drawItem in drawPaths) {
            drawItem.draw(canvas, Paint())
        }

        // Vẽ nét đang vẽ dở
        currentPath?.let { path ->
            currentDrawPaint?.let { paint -> canvas.drawPath(path, paint) }
        }

        // 3. Vẽ Stickers/Text và khung chọn
        for (item in items) {
            item.draw(canvas, Paint(Paint.ANTI_ALIAS_FLAG))

            if (item.isSelected && !isDrawMode) {
                val pts = item.getMappedPoints()

                // Vẽ khung viền nét đứt
                canvas.drawLine(pts[0], pts[1], pts[2], pts[3], borderPaint)
                canvas.drawLine(pts[2], pts[3], pts[4], pts[5], borderPaint)
                canvas.drawLine(pts[4], pts[5], pts[6], pts[7], borderPaint)
                canvas.drawLine(pts[6], pts[7], pts[0], pts[1], borderPaint)

                canvas.drawCircle(pts[0], pts[1], 25f, handlePaint)

                // Vẽ nút Resize/Rotate (Góc dưới bên phải)
                canvas.drawCircle(pts[4], pts[5], 25f, handlePaint)

                val deleteRect = RectF(
                    pts[0] - iconSize / 2, pts[1] - iconSize / 2,
                    pts[0] + iconSize / 2, pts[1] + iconSize / 2
                )

                // 3. Vẽ nút Resize/Rotate (Góc dưới - phải: pts[4], pts[5])
                val resizeRect = RectF(
                    pts[4] - iconSize / 2, pts[5] - iconSize / 2,
                    pts[4] + iconSize / 2, pts[5] + iconSize / 2
                )
                canvas.drawBitmap(resizeIcon!!, null, resizeRect, iconPaint)
                canvas.drawBitmap(deleteIcon!!, null, deleteRect, iconPaint)
            }
        }
    }

    /**
     * Xử lý tương tác người dùng
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isDrawMode) return handleDrawTouch(event)

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchMode = MODE_NONE

                // 1. Ưu tiên kiểm tra các nút chức năng của vật thể đang chọn
                selectedItem?.let { item ->
                    val pts = item.getMappedPoints()

                    // Kiểm tra nút Xóa
                    if (hypot(x - pts[0], y - pts[1]) < HANDLE_RADIUS + 20f) {
                        items.remove(item)
                        selectedItem = null
                        invalidate()
                        return true
                    }

                    // Kiểm tra nút Resize/Rotate
                    if (hypot(x - pts[4], y - pts[5]) < HANDLE_RADIUS + 20f) {
                        touchMode = MODE_RESIZE_ROTATE
                        lastX = x
                        lastY = y
                        return true
                    }
                }

                // 2. Kiểm tra chọn vật thể mới
                val hitItem = items.findLast { it.containsPoint(x, y) }
                if (hitItem != null) {
                    selectedItem = hitItem
                    items.forEach { it.isSelected = (it == hitItem) }

                    // Đưa vật thể lên lớp trên cùng
                    items.remove(hitItem)
                    items.add(hitItem)

                    touchMode = MODE_DRAG
                } else {
                    selectedItem = null
                    items.forEach { it.isSelected = false }
                }

                lastX = x
                lastY = y
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                selectedItem?.let { item ->
                    val pts = item.getMappedPoints()
                    val centerX = (pts[0] + pts[4]) / 2f
                    val centerY = (pts[1] + pts[5]) / 2f

                    when (touchMode) {
                        MODE_DRAG -> {
                            item.matrix.postTranslate(x - lastX, y - lastY)
                        }
                        MODE_RESIZE_ROTATE -> {
                            // Xử lý Scale (Co giãn)
                            val lastDist = hypot(lastX - centerX, lastY - centerY)
                            val currDist = hypot(x - centerX, y - centerY)
                            if (lastDist > 0) {
                                val scale = currDist / lastDist
                                item.matrix.postScale(scale, scale, centerX, centerY)
                            }

                            // Xử lý Rotate (Xoay)
                            val lastAngle = atan2(lastY - centerY, lastX - centerX)
                            val currAngle = atan2(y - centerY, x - centerX)
                            val degrees = Math.toDegrees((currAngle - lastAngle).toDouble()).toFloat()
                            item.matrix.postRotate(degrees, centerX, centerY)
                        }
                    }
                    lastX = x
                    lastY = y
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                touchMode = MODE_NONE
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
                    color = if (isEraserMode) Color.WHITE else brushColor
                    strokeWidth = brushSize
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    isAntiAlias = true
                }
            }
            MotionEvent.ACTION_MOVE -> { currentPath?.lineTo(x, y) }
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

    /**
     * Bộ lọc và Điều chỉnh màu sắc
     */
    fun setFilter(matrix: ColorMatrix?) {
        this.filterMatrix = matrix
        updateBasePaintFilter()
        invalidate()
    }

    fun setAdjustments(b: Float, c: Float, s: Float) {
        brightness = b
        contrast = c
        saturation = s
        updateBasePaintFilter()
        invalidate()
    }

    private fun updateBasePaintFilter() {
        val cm = ColorMatrix()
        filterMatrix?.let { cm.set(it) }

        // Ma trận điều chỉnh độ sáng và tương phản
        val adj = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness * 255f,
            0f, contrast, 0f, 0f, brightness * 255f,
            0f, 0f, contrast, 0f, brightness * 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(adj)

        // Điều chỉnh độ bão hòa
        val sat = ColorMatrix()
        sat.setSaturation(saturation)
        cm.postConcat(sat)

        basePaint.colorFilter = ColorMatrixColorFilter(cm)
    }

    /**
     * Các hàm tiện ích bổ sung
     */
    fun removeLastItemIfPending() {
        if (items.isNotEmpty()) {
            items.removeAt(items.size - 1)
            selectedItem = items.lastOrNull()
            invalidate()
        }
    }

    fun resetPreviewFilters() {
        this.filterMatrix = null
        this.brightness = 0f
        this.contrast = 1f
        this.saturation = 1f
        updateBasePaintFilter()
        invalidate()
    }
}