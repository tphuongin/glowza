package com.sgroupmobile.glowza.data.model

import android.net.Uri

class GalleryItem (
    val id: Long,
    val uri: Uri,
    val dateAdded: Long,
    val isFavourite: Boolean,
    val folderName: String,
    var isSelected: Boolean = false,
    var isVideo: Boolean = false
)