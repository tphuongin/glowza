package com.sgroupmobile.glowza.helper

import android.annotation.SuppressLint
import android.content.Context
import com.sgroupmobile.glowza.common.enum.FilterType
import com.sgroupmobile.glowza.data.model.FaceFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import javax.inject.Inject


class FilterHelper @Inject constructor(@ApplicationContext private val context: Context) {

    fun loadFilters(): List<FaceFilter> {
        val jsonString = context.assets.open("face_filters.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<FaceFilter>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val filterName = obj.getString("filter_res")
            val iconName = obj.getString("preview_icon")

            list.add(FaceFilter(
                id = obj.getInt("id"),
                name = obj.getString("name"),
                previewIcon = getResId(iconName),
                filterRes = getResId(filterName),
                type = FilterType.valueOf(obj.getString("type")),
                scaleFactor = obj.optDouble("scale", 1.0).toFloat(),
                offsetY = obj.optDouble("offset_y", 0.0).toFloat()
            ))
        }
        return list
    }

    @SuppressLint("DiscouragedApi")
    private fun getResId(resName: String): Int {
        if (resName.isEmpty()) return -1
        return context.resources.getIdentifier(resName, "drawable", context.packageName)
    }
}