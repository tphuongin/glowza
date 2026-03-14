package com.sgroupmobile.glowza.data.data_store.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class CameraDataStore @Inject constructor(
    @ApplicationContext private val context: Context) {
    //Set key
    companion object {
        val FLASH = booleanPreferencesKey("flash")
        val TIMER = intPreferencesKey("timer")
        val GRID = booleanPreferencesKey("grid")
        val CAMERA_FACING = intPreferencesKey("camera_facing")
        val RATIO = stringPreferencesKey("ratio")
    }

    //Read value
    val flash: Flow<Boolean> = context.cameraDataStore.data.map { it[FLASH]?: false }
    val timer: Flow<Int> = context.cameraDataStore.data.map { it[TIMER]?: 0 }
    val cameraFacing: Flow<CameraSelector> = context.cameraDataStore.data.map {
        val value = it[CAMERA_FACING] ?: 0
        if(value == 1){
            CameraSelector.DEFAULT_BACK_CAMERA
        } else{
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }
    val grid: Flow<Boolean> = context.cameraDataStore.data.map { it[GRID]?: false }
    val ratio: Flow<String> = context.cameraDataStore.data.map {
        it[RATIO] ?: "3:4"
    }
    suspend fun setRatio(ratioValue: String) {
        context.cameraDataStore.edit {
            it[RATIO] = ratioValue
        }
    }
    //Write value
    suspend fun setFlash(value: Boolean) {
        context.cameraDataStore.edit {
            it[FLASH] = value
        }
    }
    suspend fun setGrid(value: Boolean) {
        context.cameraDataStore.edit {
            it[GRID] = value
        }
    }
    suspend fun setTimer(value: Int) {
        context.cameraDataStore.edit {
            it[TIMER] = value
        }
    }
    suspend fun setCameraFacing(isBackCamera: Boolean) {
        val value = if(isBackCamera) 1 else 0
        context.cameraDataStore.edit {
            it[CAMERA_FACING] = value
        }
    }

}