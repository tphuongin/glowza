package com.sgroupmobile.glowza.helper

import android.annotation.SuppressLint
import android.content.Context
import com.sgroupmobile.glowza.common.enums.FilterType
import com.sgroupmobile.glowza.data.model.AppFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

class FilterHelper @Inject constructor(@ApplicationContext private val context: Context) {

    fun loadAllFilters(): Map<String, List<AppFilter>> {
        val jsonString = context.assets.open("face_filters.json").bufferedReader().use { it.readText() }
        val rootObject = JSONObject(jsonString)
        val result = mutableMapOf<String, List<AppFilter>>()

        // Face Filters (Stickers)
        val faceArray = rootObject.optJSONArray("face_filters")
        val faceList = mutableListOf<AppFilter>()
        if (faceArray != null) {
            for (i in 0 until faceArray.length()) {
                val obj = faceArray.getJSONObject(i)
                faceList.add(parseAppFilter(obj, isColor = false))
            }
        }
        result["face_filters"] = faceList

        // Color Filters (Hiệu ứng màu)
        val colorArray = rootObject.optJSONArray("color_filters")
        val colorList = mutableListOf<AppFilter>()
        if (colorArray != null) {
            for (i in 0 until colorArray.length()) {
                val obj = colorArray.getJSONObject(i)
                colorList.add(parseAppFilter(obj, isColor = true))
            }
        }
        result["color_filters"] = colorList

        return result
    }

    /**
     * Hàm phụ trợ để parse dữ liệu JSON sang AppFilter
     */
    private fun parseAppFilter(obj: JSONObject, isColor: Boolean): AppFilter {
        val previewIcon = obj.getString("preview_icon")

        return if (isColor) {
            // Trường hợp là Filter màu
            AppFilter(
                id = obj.getInt("id"),
                name = obj.getString("name"),
                previewIcon = getResId(previewIcon),
                colorCode = obj.getString("color_code")
            )
        } else {
            // Trường hợp là Face Filter (Sticker)
            val filterRes = obj.getString("filter_res")
            AppFilter(
                id = obj.getInt("id"),
                name = obj.getString("name"),
                previewIcon = getResId(previewIcon),
                filterRes = getResId(filterRes),
                type = FilterType.valueOf(obj.getString("type")),
                scaleFactor = obj.optDouble("scale", 1.0).toFloat(),
                offsetY = obj.optDouble("offset_y", 0.0).toFloat()
            )
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun getResId(resName: String): Int {
        if (resName.isEmpty()) return -1
        return context.resources.getIdentifier(resName, "drawable", context.packageName)
    }
}