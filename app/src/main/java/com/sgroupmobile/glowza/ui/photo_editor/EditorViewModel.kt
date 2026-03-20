package com.sgroupmobile.glowza.ui.photo_editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import com.sgroupmobile.glowza.base.BaseViewModel
import com.sgroupmobile.glowza.common.enums.ToolType
import com.sgroupmobile.glowza.data.model.EditorAction
import com.sgroupmobile.glowza.data.model.ImageFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.graphics.scale
import com.sgroupmobile.glowza.util.FilterUtils
import androidx.core.graphics.createBitmap

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val application: Application
) : BaseViewModel() {
    private var originalBitmap: Bitmap? = null
    private var editorEngine: EditorEngine? = null

    // Ảnh hiển thị cuối cùng sau khi render xong các layer
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap = _previewBitmap.asStateFlow()
    private val _filterPreviews = MutableStateFlow<List<ImageFilter>>(emptyList())
    val filterPreviews = _filterPreviews.asStateFlow()

    private val _currentTool = MutableStateFlow<ToolType?>(null)
    val currentTool = _currentTool.asStateFlow()

    private val _currentUri = MutableStateFlow<Uri?>(null)
    val currentUri = _currentUri.asStateFlow()

    // Stack quản lý Undo/Redo
    private val undoStack = mutableListOf<EditorAction>()
    private val redoStack = mutableListOf<EditorAction>()

    private val _navigationState = MutableStateFlow(NavigationState(
        canUndo = false,
        canRedo = false
    ))
    val navigationState = _navigationState.asStateFlow()

    data class NavigationState(val canUndo: Boolean, val canRedo: Boolean)
    fun loadImage(uri: Uri) {
        clearFilterPreviews()
        _currentUri.value = uri
        launch(Dispatchers.Default) {
            updateLoading()
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val inputStream = application.contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
                } catch (e: Exception) {
                    null
                }
            }
            originalBitmap = bitmap
            bitmap?.let { editorEngine = EditorEngine(it) }
            _previewBitmap.value = bitmap
            updateLoading()
        }
    }

    // Thêm thao tác mới
    fun addAction(action: EditorAction) {
        undoStack.add(action)
        redoStack.clear() // Khi có action mới, không thể Redo các bước cũ
        renderImage()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(undoStack.removeAt(undoStack.lastIndex))
            renderImage()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(redoStack.removeAt(redoStack.lastIndex))
            renderImage()
        }
    }
    fun prepareFilterPreviews() {
        // Lấy ảnh hiện tại đang hiển thị (đã có thể qua Crop/Sticker...)
        val originalBitmap = originalBitmap ?: return

        launch(Dispatchers.Default) {
            // Bước 1: Tạo thumbnail nhỏ (150x150) để tránh lag và tốn RAM
            val thumbSize = 150
            val thumbnail = createCenterCropThumbnail(originalBitmap, thumbSize)

            // Bước 2: Lấy danh sách Filter từ Utils
            val filters = FilterUtils.getListImageFilter()

            // Bước 3: Chạy vòng lặp áp dụng từng Filter lên thumbnail
            filters.forEach { filter ->
                filter.imageBitmap = applyFilterToThumbnail(thumbnail, filter.colorMatrix)
            }

            // Bước 4: Cập nhật Flow để Fragment nhận được dữ liệu
            _filterPreviews.value = filters
        }
    }
    fun clearFilterPreviews() {
        _filterPreviews.value = emptyList()
    }
    private fun createCenterCropThumbnail(src: Bitmap, size: Int): Bitmap {
        val width = src.width
        val height = src.height

        // Tính toán tỉ lệ để cắt
        val scale = if (width > height) size.toFloat() / height else size.toFloat() / width
        val newWidth = (scale * width).toInt()
        val newHeight = (scale * height).toInt()

        // Resize ảnh giữ nguyên tỉ lệ trước
        val scaledBitmap = Bitmap.createScaledBitmap(src, newWidth, newHeight, true)

        // Cắt lấy phần giữa (Center Crop) thành hình vuông
        val xOffset = (newWidth - size) / 2
        val yOffset = (newHeight - size) / 2

        val result = Bitmap.createBitmap(scaledBitmap, xOffset, yOffset, size, size)

        // Giải phóng bộ nhớ cho bitmap trung gian
        if (scaledBitmap != result) scaledBitmap.recycle()

        return result
    }

    private fun applyFilterToThumbnail(src: Bitmap, matrix: ColorMatrix?): Bitmap {
        val bitmap = createBitmap(src.width, src.height)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            matrix?.let { colorFilter = ColorMatrixColorFilter(it) }
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return bitmap
    }
    fun renderImage() {
        val engine = editorEngine ?: return
        launch(Dispatchers.Default) {
            val result = withContext(Dispatchers.Default) {
                engine.render(undoStack)
            }
            _previewBitmap.value = result

            // SỬA TẠI ĐÂY: Lấy giá trị thực tế của Stack
            _navigationState.value = NavigationState(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    // Thêm hàm dọn dẹp khi load ảnh mới hoàn toàn
    fun clearAllActions() {
        undoStack.clear()
        redoStack.clear()
        _navigationState.value = NavigationState(false, false)
    }

    fun selectTool(type: ToolType? = null) {
        _currentTool.value = type
    }
    fun resetTool(){
        _currentTool.value = null
    }
}