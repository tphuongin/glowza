package com.sgroupmobile.glowza.ui.camera

import androidx.camera.core.CameraSelector
import androidx.lifecycle.viewModelScope
import com.sgroupmobile.glowza.base.BaseViewModel
import com.sgroupmobile.glowza.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: CameraRepository
): BaseViewModel() {

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