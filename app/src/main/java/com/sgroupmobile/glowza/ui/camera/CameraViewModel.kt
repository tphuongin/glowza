package com.sgroupmobile.glowza.ui.camera

import androidx.camera.core.CameraSelector
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.face.Face
import com.sgroupmobile.glowza.base.BaseViewModel
import com.sgroupmobile.glowza.common.enums.CameraMode
import com.sgroupmobile.glowza.data.model.AppFilter
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
    private val _cameraMode = MutableStateFlow(CameraMode.PHOTO)
    val cameraMode: StateFlow<CameraMode> = _cameraMode.asStateFlow()
    private val _isFaceFilterOn = MutableStateFlow(false)
    val isFaceFilterOn = _isFaceFilterOn.asStateFlow()

    // Danh sách khuôn mặt ML Kit trả về
    private val _faces = MutableStateFlow<List<Face>>(emptyList())
    val faces = _faces.asStateFlow()
    private val _selectedAppFilter = MutableStateFlow<AppFilter?>(null)
    val selectedFaceFilter = _selectedAppFilter.asStateFlow()
    private val _selectedColorFilter = MutableStateFlow<String>("")
    val selectedColorFilter = _selectedColorFilter.asStateFlow()
    private val _currentTabFilters = MutableStateFlow<List<AppFilter>>(emptyList())
    val currentTabFilters = _currentTabFilters.asStateFlow()

    // Kích thước nguồn ảnh để scale tọa độ vẽ
    var imageSourceWidth = 0
    var imageSourceHeight = 0

    fun updateCameraMode(newMode: CameraMode){
        _cameraMode.value = newMode
    }

    // Logic chuyển đổi dữ liệu hiển thị giữa 2 Tab
    fun setFilterTab(isColorTab: Boolean, allFilters: Map<String, List<AppFilter>>) {
        _currentTabFilters.value = if (isColorTab) {
            allFilters["color_filters"] ?: emptyList()
        } else {
            allFilters["face_filters"] ?: emptyList()
        }
    }

    fun setSelectedColorFilter(code: String) {
        _selectedColorFilter.value = code
    }

    fun toggleFaceFilter() {
        _isFaceFilterOn.value = !_isFaceFilterOn.value
    }

    fun setSelectedFaceFilter(filter: AppFilter?) {
        _selectedAppFilter.value = filter
        if (filter != null && !_isFaceFilterOn.value) {
            _isFaceFilterOn.value = true
        }
    }
    fun updateFaces(faces: List<Face>, width: Int, height: Int) {
        imageSourceWidth = width
        imageSourceHeight = height
        _faces.value = faces
    }


    private val _isZoomIn = MutableStateFlow(true)
    val isZoomIn: StateFlow<Boolean> = _isZoomIn

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

    fun updateZoom() {
        _isZoomIn.value = !isZoomIn.value
    }

    fun setRatio(value: String) {
        launch { repository.setRatio(value) }
    }

    fun toggleFlash() {
        launch { repository.setFlash(!flash.value) }
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