package com.sgroupmobile.glowza.ui.gallery

import androidx.lifecycle.viewModelScope
import com.sgroupmobile.glowza.base.BaseViewModel
import com.sgroupmobile.glowza.data.model.GalleryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GalleryViewModel : BaseViewModel() {

    private val _images = MutableStateFlow<List<GalleryItem>>(emptyList())
    val images = _images.asStateFlow()

    private val _videos = MutableStateFlow<List<GalleryItem>>(emptyList())
    val videos = _videos.asStateFlow()

    fun setImages(list: List<GalleryItem>) {
        _images.value = list
    }

    fun setVideos(list: List<GalleryItem>) {
        _videos.value = list
    }

    val favoritePhotos = _images.map { list ->
        list.filter { it.isFavourite }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val glowzaPhotos = _images.map { list ->
        list.filter { it.folderName.contains("Glowza", ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}