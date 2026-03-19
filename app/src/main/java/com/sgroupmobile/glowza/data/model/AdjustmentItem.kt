package com.sgroupmobile.glowza.data.model

import com.sgroupmobile.glowza.common.enums.AdjustType

data class AdjustmentItem(
    val name: String,
    val type: AdjustType,
    val icon: Int,
    var currentValue: Float, // Giá trị hiện tại
    val minValue: Float,    // Giá trị tối thiểu (ví dụ: -1.0f)
    val maxValue: Float,    // Giá trị tối đa (ví dụ: 1.0f)
    val defaultValue: Float, // Giá trị mặc định (ví dụ: 0.0f)
    var isSelected: Boolean = false
)