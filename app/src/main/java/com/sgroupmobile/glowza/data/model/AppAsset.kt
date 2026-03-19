package com.sgroupmobile.glowza.data.model

data class AppAsset(
    val id: Int,
    val name: String,
    val previewRes: Int,
    val mainRes: Int,
    val type: AssetType,

    var scale: Float = 1.0f,
    var opacity: Int = 255
)

enum class AssetType {
    STICKER, FRAME, FILTER
}