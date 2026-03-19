package com.sgroupmobile.glowza.data.model

import android.graphics.ColorMatrix

data class ImageFilter(
    val name: String,
    val colorMatrix: ColorMatrix?,
    var isSelected: Boolean = false
)