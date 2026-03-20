package com.sgroupmobile.glowza.provider

import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.common.enums.EditorTool
import com.sgroupmobile.glowza.common.enums.ToolType

object EditorToolProvider {
    fun getPublicTools(): List<EditorTool> {
        return listOf(
            EditorTool(ToolType.CROP, R.drawable.ic_crop, R.string.tool_crop),
            EditorTool(ToolType.FILTER, R.drawable.ic_filter, R.string.tool_filter),
            EditorTool(ToolType.ADJUST, R.drawable.ic_adjust, R.string.tool_adjust),
            EditorTool(ToolType.STICKER, R.drawable.ic_sticker, R.string.tool_sticker),
            EditorTool(ToolType.TEXT, R.drawable.ic_text, R.string.tool_text),
            EditorTool(ToolType.DRAW, R.drawable.ic_draw, R.string.tool_draw),
            EditorTool(ToolType.FRAME, R.drawable.ic_frame, R.string.tool_frame),
        )
    }
}