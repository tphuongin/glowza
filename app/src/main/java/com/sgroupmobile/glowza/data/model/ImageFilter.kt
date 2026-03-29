package com.sgroupmobile.glowza.data.model

import android.graphics.Bitmap
import android.graphics.ColorMatrix

data class ImageFilter(
    override val id: Int,
    override val displayName: String,
    val colorMatrix: ColorMatrix?,
    override var imageBitmap: Bitmap? = null
) : DisplayableItem {
    override val imageRes: Int? = null
}