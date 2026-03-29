package com.sgroupmobile.glowza.repository

import androidx.camera.core.CameraSelector
import com.sgroupmobile.glowza.data.data_store.camera.CameraDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraRepository @Inject constructor(
    private val dataStore: CameraDataStore
) {
    val flash: Flow<Boolean> = dataStore.flash
    val grid: Flow<Boolean> = dataStore.grid
    val timer: Flow<Int> = dataStore.timer
    val cameraFacing: Flow<CameraSelector> = dataStore.cameraFacing
    val ratio: Flow<String> = dataStore.ratio

    suspend fun setRatio(value: String) = dataStore.setRatio(value)
    suspend fun setFlash(value: Boolean) {
        dataStore.setFlash(value)
    }
    suspend fun setGrid(value: Boolean) {
        dataStore.setGrid(value)
    }
    suspend fun setTimer(value: Int) {
        dataStore.setTimer(value)
    }
    suspend fun setCameraFacing(isBackCamera: Boolean) {
        dataStore.setCameraFacing(isBackCamera)
    }
}