package com.sgroupmobile.glowza.data.model

import com.sgroupmobile.glowza.common.enum.FilterType

data class FaceFilter(
    val id: Int,
    val name: String,
    val previewIcon: Int,
    val filterRes: Int,
    val type: FilterType,
    val scaleFactor: Float,
    val offsetY: Float = 0f // Độ lệch dọc để căn chỉnh cho chuẩn
)