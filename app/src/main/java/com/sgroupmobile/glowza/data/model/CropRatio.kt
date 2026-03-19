package com.sgroupmobile.glowza.data.model

data class CropRatio(
    val name: String,
    val ratioX: Float, // Ví dụ: 3f
    val ratioY: Float, // Ví dụ: 4f
    val icon: Int,
    var isSelected: Boolean = false
) {
    // Trả về giá trị float của tỷ lệ (ví dụ: 0.75)
    fun getRatioValue(): Float = if (ratioX == 0f) 0f else ratioX / ratioY
}