package com.sgroupmobile.glowza.data.model

import android.net.Uri

class GalleryImage (
    val id: Long,
    val uri: Uri,
    val dateAdded: Long,
    val isFavourite: Boolean,
    val folderName: String
)