package com.sgroupmobile.glowza.ui.photo_editor

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
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
import com.sgroupmobile.glowza.util.FilterUtils
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.sgroupmobile.glowza.base.BaseItem
import com.sgroupmobile.glowza.data.model.StickerItem
import com.sgroupmobile.glowza.data.model.TextItem
import dagger.hilt.android.internal.Contexts
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

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
    private val _itemList = MutableStateFlow<List<BaseItem>>(emptyList())
    val itemList = _itemList.asStateFlow()
    private val _exportStatus = MutableStateFlow<Uri?>(null)
    val exportStatus = _exportStatus.asStateFlow()
    private val _currentTool = MutableSharedFlow<ToolType?>()
    val currentTool = _currentTool.asSharedFlow()
    private val _currentUri = MutableStateFlow<Uri?>(null)
    val currentUri = _currentUri.asStateFlow()

    // Stack quản lý Undo/Redo
    private var undoStack = mutableListOf<EditorAction>()
    private val redoStack = mutableListOf<EditorAction>()

    private val _navigationState = MutableStateFlow(
        NavigationState(
            canUndo = false,
            canRedo = false
        )
    )
    val navigationState = _navigationState.asStateFlow()

    data class NavigationState(val canUndo: Boolean, val canRedo: Boolean)

    fun loadImage(uri: Uri) {
        _currentUri.value = uri
        launch(Dispatchers.IO) {
            updateLoading()
            try {
                // Đọc kích thước thật của file
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                application.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }

                //Tính toán SampleSize để giảm ngay từ khi đọc file
                val reqSize = 1600
                options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize)
                options.inJustDecodeBounds = false

                //Load ảnh đã scale lần 1 vào RAM
                var loadedBitmap = application.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }

                // Ép nó về đúng ngưỡng an toàn
                loadedBitmap = loadedBitmap?.let {
                    val scaled = scaleBitmapWithLimit(it, reqSize.toFloat())
                    //giải phóng cái cũ
                    if (scaled != it) it.recycle()
                    scaled
                }

                withContext(Dispatchers.Main) {
                    originalBitmap?.recycle()

                    originalBitmap = loadedBitmap
                    loadedBitmap?.let {
                        editorEngine = EditorEngine(it)
                        _previewBitmap.value = it
                    }
                    updateLoading()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun scaleBitmapWithLimit(src: Bitmap, maxLimit: Float): Bitmap {
        val width = src.width
        val height = src.height
        val maxSide = Math.max(width, height)
        if (maxSide <= maxLimit) return src

        val scale = maxLimit / maxSide
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(src, 0, 0, width, height, matrix, true)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun addAction(action: EditorAction) {
        undoStack.add(action)
        redoStack.clear()
        renderImage()
    }

    fun removeLastItem() {
        val currentList = _itemList.value
        if (currentList.isNotEmpty()) {
            _itemList.value = currentList.dropLast(1)
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val action = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(action)
            if (action is EditorAction.Sticker || action is EditorAction.Text) {
                removeLastItem()
            }
            renderImage()
        }
    }

    fun removeItemById(targetId: Long) {
        _itemList.update { currentList ->
            currentList.filter { it.id != targetId }
        }

        val iterator = undoStack.iterator()
        while (iterator.hasNext()) {
            val action = iterator.next()
            if (action is EditorAction.Sticker && action.id == targetId) {
                iterator.remove()
                break
            } else if (action is EditorAction.Text && action.id == targetId) {
                iterator.remove()
                break
            }
        }
        renderImage()
    }
    fun addNewItem(item: BaseItem) {
        _itemList.value += item
    }
    fun updateTextAction(item: TextItem, displayMatrix: Matrix) {
        val index = undoStack.indexOfLast { it is EditorAction.Text && it.id == item.id }

        if (index != -1) {
            val updatedAction = EditorAction.Text(
                text = item.text,
                matrix = Matrix(item.matrix),
                textPaint = Paint(item.textPaint),
                id = item.id,
                displayMatrix = Matrix(displayMatrix)
            )

            undoStack[index] = updatedAction
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val action = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(action)

            when (action) {
                is EditorAction.Sticker -> {
                    val item = StickerItem(action.sticker).apply {
                        this.id = action.id
                        this.matrix.set(action.matrix)
                    }
                    addNewItem(item)
                }
                is EditorAction.Text -> {
                    val item = TextItem(action.text).apply {
                        this.id = action.id
                        this.matrix.set(action.matrix)
                        this.text = action.text
                        this.textPaint.set(action.textPaint)
                    }
                    addNewItem(item)
                }
                else -> {  }
            }
            renderImage()
        }
    }


    fun prepareFilterPreviews(context: Context) {
        val originalBitmap = originalBitmap ?: return

        launch(Dispatchers.Default) {
            // Bước 1: Tạo thumbnail nhỏ (150x150) để tránh lag và tốn RAM
            val thumbSize = 150
            val thumbnail = createCenterCropThumbnail(originalBitmap, thumbSize)
            // Bước 2: Lấy danh sách Filter từ Utils
            val filters = FilterUtils.getListImageFilter(context)

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

            _navigationState.value = NavigationState(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun clearAllActions() {
        undoStack.clear()
        redoStack.clear()
        _navigationState.value = NavigationState(false, false)
    }

    suspend fun selectTool(type: ToolType? = null) {
        _currentTool.emit(type)
    }

    suspend fun resetTool() {
        _currentTool.emit(null)
    }

    fun saveImage() {
        val engine = editorEngine ?: return
        viewModelScope.launch(Dispatchers.Default) {
            updateLoading()
            try {
                // Render với isExporting = true để vẽ Sticker/Text
                val finalBitmap = engine.render(undoStack, isExporting = true)

                // Lưu vào file tạm
                val cacheFile = File(application.cacheDir, "temp_export.jpg")
                application.contentResolver.openOutputStream(cacheFile.toUri())?.use {
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }

                // Gọi hàm util
                val galleryUri = com.sgroupmobile.glowza.util.saveImageToGallery(
                    application,
                    cacheFile.toUri(),
                    "Glowza_${System.currentTimeMillis()}"
                )

                withContext(Dispatchers.Main) {
                    _exportStatus.value = galleryUri
                    updateLoading()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _exportStatus.value = null
                    updateLoading()
                }
            }
        }
    }
    fun resetExportStatus(){
        _exportStatus.value = null
    }
}