package com.sgroupmobile.glowza.data.model

import com.sgroupmobile.glowza.common.enum.FilterType
data class AppFilter(
    val id: Int,
    val name: String,
    val previewIcon: Int,

    //  (Face Overlay) ---
    val filterRes: Int = -1,
    val type: FilterType = FilterType.NOSE,
    val scaleFactor: Float = 1.0f,
    val offsetY: Float = 0f,

    // (Image Filter)
    val colorCode: String = ""  // Mã định danh cho GPUImage (VD: "PASTEL", "SEPIA")
) {
    val isColorFilter: Boolean
        get() = colorCode.isNotEmpty()
}