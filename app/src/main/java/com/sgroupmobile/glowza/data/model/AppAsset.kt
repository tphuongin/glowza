package com.sgroupmobile.glowza.data.model

import android.graphics.Bitmap

data class AppAsset(
    override val id: Int,
    val mainRes: Int,
    override var displayName: String = "",
    val type: AssetType
) : DisplayableItem {
    override var imageRes: Int = mainRes
    override var imageBitmap: Bitmap? = null
}

enum class AssetType {
    STICKER, FRAME
}