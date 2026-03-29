package com.sgroupmobile.glowza.ui.gallery

import com.sgroupmobile.glowza.base.BaseViewModel
import com.sgroupmobile.glowza.data.model.GalleryImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class GalleryViewModel: BaseViewModel() {

    private val _allPhotos = MutableStateFlow<List<GalleryImage>>(emptyList())
    val allPhotos = _allPhotos.asStateFlow()

    fun setAllPhotos(list: List<GalleryImage>) {
        _allPhotos.value = list
    }
    val favoritePhotos = _allPhotos.map { list ->
        list.filter { it.isFavourite }
    }

    val glowzaPhotos = _allPhotos.map { list ->
        list.filter { it.folderName.contains("Glowza", ignoreCase = true) }
    }
}