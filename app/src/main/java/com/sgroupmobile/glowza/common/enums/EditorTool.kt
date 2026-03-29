package com.sgroupmobile.glowza.common.enums

data class EditorTool(
    val type: ToolType,
    val icon: Int,
    val nameRes: Int
)

enum class ToolType {
    CROP, DRAW, STICKER, TEXT, FILTER, ADJUST, FRAME
}