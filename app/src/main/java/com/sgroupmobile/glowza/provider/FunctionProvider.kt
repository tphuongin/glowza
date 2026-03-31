package com.sgroupmobile.glowza.provider

import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.data.model.FunctionItem
import com.sgroupmobile.glowza.data.model.FunctionType

object FunctionProvider {
    fun getFunctions() = listOf(
        FunctionItem(R.string.func_edit, R.drawable.ic_editor, FunctionType.EDIT,true),
        FunctionItem(R.string.func_camera, R.drawable.ic_camera, FunctionType.CAMERA,true),
        FunctionItem(R.string.func_my_photos, R.drawable.ic_images, FunctionType.IMAGES),
        FunctionItem(R.string.func_stitch, R.drawable.ic_collage, FunctionType.PHOTO_COLLAGE),
        FunctionItem(R.string.func_photobooth, R.drawable.ic_photobooth, FunctionType.PHOTOBOOTH),
        FunctionItem(R.string.func_my_videos, R.drawable.ic_video, FunctionType.VIDEOS)
    )
}