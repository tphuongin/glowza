package com.sgroupmobile.glowza.data.model

import android.graphics.RectF

data class CollageLayout(
    val id: Int,
    val slots: List<RectF> // vùng hiển thị ảnh (0f -> 1f)
)