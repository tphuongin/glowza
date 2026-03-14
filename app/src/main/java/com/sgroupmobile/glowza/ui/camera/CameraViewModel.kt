package com.sgroupmobile.glowza.ui.camera

import androidx.camera.core.CameraSelector
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.face.Face
import com.sgroupmobile.glowza.base.BaseViewModel
import com.sgroupmobile.glowza.data.model.FaceFilter
import com.sgroupmobile.glowza.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: CameraRepository
): BaseViewModel() {
    private val _isFaceFilterOn = MutableStateFlow(false)
    val isFaceFilterOn = _isFaceFilterOn.asStateFlow()

    private val _faces = MutableStateFlow<List<Face>>(emptyList())
    val faces = _faces.asStateFlow()

    private val _selectedFaceFilter = MutableStateFlow<FaceFilter?>(null)
    val selectedFaceFilter = _selectedFaceFilter.asStateFlow()

    // Lưu kích thước ảnh gốc từ ML Kit để tính tỉ lệ scale
    var imageSourceWidth = 0
    var imageSourceHeight = 0

    fun toggleFaceFilter() {
        _isFaceFilterOn.value = !_isFaceFilterOn.value
    }

    fun setSelectedFaceFilter(filter: FaceFilter?) {
        _selectedFaceFilter.value = filter
    }

    fun updateFaces(faces: List<Face>, width: Int, height: Int) {
        imageSourceWidth = width
        imageSourceHeight = height
        _faces.value = faces
    }
    private val _isZoomIn = MutableStateFlow(true)
    val isZoomIn: StateFlow<Boolean> = _isZoomIn
    // Chuyển đổi Flow thành StateFlow để UI và Controller lấy giá trị nhanh chóng
    val flash = repository.flash
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val grid = repository.grid
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val timer = repository.timer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cameraFacing = repository.cameraFacing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CameraSelector.DEFAULT_BACK_CAMERA)

    val ratio = repository.ratio
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "3:4")

    fun updateZoom(){
        _isZoomIn.value = !isZoomIn.value
    }
    fun setRatio(value: String) {
        launch { repository.setRatio(value) }
    }

    fun toggleFlash() {
        launch {
            // Lấy giá trị hiện tại trực tiếp từ StateFlow mà không cần dùng .first() của Flow cũ
            repository.setFlash(!flash.value)
        }
    }

    fun toggleGrid(value: Boolean) {
        launch { repository.setGrid(value) }
    }

    fun setTimer(value: Int) {
        launch { repository.setTimer(value) }
    }

    fun switchCamera() {
        launch {
            val isBackCamera = cameraFacing.value == CameraSelector.DEFAULT_BACK_CAMERA
            repository.setCameraFacing(!isBackCamera)
        }
    }
}