package com.sgroupmobile.glowza.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseItem
import com.sgroupmobile.glowza.common.enums.Constants.HANDLE_RADIUS
import com.sgroupmobile.glowza.common.enums.Constants.MODE_DRAG
import com.sgroupmobile.glowza.common.enums.Constants.MODE_NONE
import com.sgroupmobile.glowza.common.enums.Constants.MODE_RESIZE_ROTATE
import com.sgroupmobile.glowza.data.model.DrawItem
import com.sgroupmobile.glowza.data.model.TextItem
import com.sgroupmobile.glowza.ui.photo_editor.activity.EditorActivity
import kotlin.math.atan2
import kotlin.math.hypot

class EditorView(context: Context, attrs: AttributeSet) : View(context, attrs) {

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
    private var framePreviewBitmap: Bitmap? = null

    private val imageBounds = RectF()

    var onTextItemDoubleClicked: ((TextItem) -> Unit)? = null

    // Paint để vẽ Bitmap mượt hơn
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val iconSize = 50f

    // Stickers/Text
    private var items = mutableListOf<BaseItem>()
    private var selectedItem: BaseItem? = null
    private var touchMode = MODE_NONE
    private val redoPaths = mutableListOf<DrawItem>()
    private var lastX = 0f
    private var lastY = 0f
    fun getDrawPaths(): List<DrawItem> {
        return drawPaths.toList()
    }
    fun undoLastDraw() {
        if (drawPaths.isNotEmpty()) {
            val last = drawPaths.removeAt(drawPaths.size - 1)
            redoPaths.add(last)
            invalidate()
        }
    }
    fun setFramePreview(bitmap: Bitmap?) {
        this.framePreviewBitmap = bitmap
        invalidate()
    }
    fun getBaseMatrix(): Matrix = baseMatrix
    fun setSelectedItem(item: BaseItem?) {
        items.forEach { it.isSelected = false }

        this.selectedItem = item
        item?.isSelected = true

        item?.let {
            items.remove(it)
            items.add(it)
        }

        invalidate()
    }
    init {
        // Ép View vẽ bằng Software để không bị giới hạn 100MB của Canvas phần cứng
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }
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

    //Filter & Adjustments
    private var filterMatrix: ColorMatrix? = null
    private var brightness = 0f
    private var contrast = 1f
    private var saturation = 1f
    private val deleteIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_remove)
    private val resizeIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_rotate)

    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val hitItem = items.findLast { it.containsPoint(e.x, e.y) }
                if (hitItem is TextItem) {
                    onTextItemDoubleClicked?.invoke(hitItem)
                    return true
                }
                return false
            }
        })

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

    fun redoLastDraw() {
        if (redoPaths.isNotEmpty()) {
            val lastRedo = redoPaths.removeAt(redoPaths.size - 1)
            drawPaths.add(lastRedo)
            invalidate()
        }
    }
    fun clearAllDraw() {
        drawPaths.clear()
        invalidate()
    }

    fun setBaseBitmap(bitmap: Bitmap) {
        this.baseBitmap = bitmap
        post {
            val scale = (width.toFloat() / bitmap.width).coerceAtMost(height.toFloat() / bitmap.height)
            baseMatrix.reset()
            baseMatrix.postScale(scale, scale)
            val dx = (width - bitmap.width * scale) / 2f
            val dy = (height - bitmap.height * scale) / 2f
            baseMatrix.postTranslate(dx, dy)
            // vùng giới hạn của ảnh
            imageBounds.set(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            baseMatrix.mapRect(imageBounds)
            invalidate()
        }
    }
    fun updateItems(newList: List<BaseItem>) {
        this.items.clear()
        this.items.addAll(newList)

        // Cho xuat hien ở giữa nếu lần đầu
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
            invalidate()
        }

        if (width > 0 && height > 0) {
            setupMatrix()
        } else {
            post { setupMatrix() }
        }
    }

    fun getSelectedItem(): BaseItem? = selectedItem

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Vẽ ảnh gốc
        baseBitmap?.let { canvas.drawBitmap(it, baseMatrix, basePaint) }

        // giới hạn vùng vẽ
        canvas.save()
        canvas.clipRect(imageBounds)

        val drawingLayer = canvas.saveLayer(imageBounds, null)
        for (drawItem in drawPaths) {
            drawItem.draw(canvas, Paint())
        }
        currentPath?.let { path ->
            currentDrawPaint?.let { paint -> canvas.drawPath(path, paint) }
        }
        canvas.restoreToCount(drawingLayer)

        for (item in items) {
            item.draw(canvas, Paint(Paint.ANTI_ALIAS_FLAG))
        }

        framePreviewBitmap?.let { frame ->
            canvas.drawBitmap(frame, null, imageBounds, null)
        }

        canvas.restore()

        for (item in items) {
            if (item.isSelected && !isDrawMode) {
                val pts = item.getMappedPoints()

                // Vẽ viền
                canvas.drawLine(pts[0], pts[1], pts[2], pts[3], borderPaint)
                canvas.drawLine(pts[2], pts[3], pts[4], pts[5], borderPaint)
                canvas.drawLine(pts[4], pts[5], pts[6], pts[7], borderPaint)
                canvas.drawLine(pts[6], pts[7], pts[0], pts[1], borderPaint)

                canvas.drawCircle(pts[0], pts[1], 25f, handlePaint)
                canvas.drawCircle(pts[4], pts[5], 25f, handlePaint)

                deleteIcon?.let {
                    val deleteRect = RectF(pts[0] - iconSize/2, pts[1] - iconSize/2, pts[0] + iconSize/2, pts[1] + iconSize/2)
                    canvas.drawBitmap(it, null, deleteRect, iconPaint)
                }
                resizeIcon?.let {
                    val resizeRect = RectF(pts[4] - iconSize/2, pts[5] - iconSize/2, pts[4] + iconSize/2, pts[5] + iconSize/2)
                    canvas.drawBitmap(it, null, resizeRect, iconPaint)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) return true
        if (isDrawMode) return handleDrawTouch(event)

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchMode = MODE_NONE

                selectedItem?.let { item ->
                    val pts = item.getMappedPoints()

                    // Xóa
                    if (hypot(x - pts[0], y - pts[1]) < HANDLE_RADIUS + 20f) {
                        items.remove(item)
                        (context as? EditorActivity)?.viewModel?.removeItemById(item.id)

                        selectedItem = null
                        invalidate()
                        return true
                    }

                    // Resize/Rotate
                    if (hypot(x - pts[4], y - pts[5]) < HANDLE_RADIUS + 20f) {
                        touchMode = MODE_RESIZE_ROTATE
                        lastX = x
                        lastY = y
                        return true
                    }
                }

                //chọn vật thể mới
                val hitItem = items.findLast { it.containsPoint(x, y) }
                if (hitItem != null) {
                    selectedItem = hitItem
                    items.forEach { it.isSelected = (it == hitItem) }

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
                            // Xử lý Scale
                            val lastDist = hypot(lastX - centerX, lastY - centerY)
                            val currDist = hypot(x - centerX, y - centerY)
                            if (lastDist > 0) {
                                val scale = currDist / lastDist
                                item.matrix.postScale(scale, scale, centerX, centerY)
                            }

                            // Xử lý Rotate
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

        // Chỉ cho phép vẽ nếu chạm bên trong vùng ảnh
        if (!imageBounds.contains(x, y) && event.action == MotionEvent.ACTION_DOWN) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                redoPaths.clear()
                currentPath = Path().apply { moveTo(x, y) }
                currentDrawPaint = Paint().apply {
                    isAntiAlias = true
                    strokeWidth = brushSize
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND

                    if (isEraserMode) {
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    } else {
                        color = brushColor
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val constrainedX = x.coerceIn(imageBounds.left, imageBounds.right)
                val constrainedY = y.coerceIn(imageBounds.top, imageBounds.bottom)
                currentPath?.lineTo(constrainedX, constrainedY)
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

        val sat = ColorMatrix()
        sat.setSaturation(saturation)
        cm.postConcat(sat)

        basePaint.colorFilter = ColorMatrixColorFilter(cm)
    }
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