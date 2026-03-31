package com.sgroupmobile.glowza.data.model

data class FunctionItem(
    val nameRes: Int,
    val icon: Int,
    val type: FunctionType,
    val isBig: Boolean = false,
)
enum class FunctionType {
    EDIT, CAMERA, IMAGES, PHOTO_COLLAGE, PHOTOBOOTH, VIDEOS
}