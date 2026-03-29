package com.sgroupmobile.glowza.data.model

import android.graphics.Bitmap

interface DisplayableItem {
    val id: Int
    val displayName: String
    val imageRes: Int?      // Dùng cho Sticker/Frame (Resource ID)
    val imageBitmap: Bitmap? // Dùng cho Filter Preview (Bitmap)
}