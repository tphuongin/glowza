package com.sgroupmobile.glowza.ui.photo_editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.sgroupmobile.glowza.base.BaseViewModel
import com.sgroupmobile.glowza.common.enums.ToolType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val application: Application
) : BaseViewModel() {

    private val _baseBitmap = MutableStateFlow<Bitmap?>(null)
    val baseBitmap = _baseBitmap.asStateFlow()

    private val _currentTool = MutableStateFlow<ToolType?>(null)
    val currentTool = _currentTool.asStateFlow()

    fun loadImage(uri: Uri) {
        launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val inputStream = application.contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
                } catch (e: Exception) {
                    null
                }
            }
            _baseBitmap.value = bitmap
        }
    }

    fun selectTool(type: ToolType) {
        _currentTool.value = type
    }
}