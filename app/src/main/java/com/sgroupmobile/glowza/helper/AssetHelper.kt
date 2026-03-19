package com.sgroupmobile.glowza.helper

import android.content.Context
import com.sgroupmobile.glowza.data.model.AppAsset
import com.sgroupmobile.glowza.data.model.AssetType
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

class AssetHelper @Inject constructor(@ApplicationContext private val context: Context) {

    fun loadAssets(fileName: String): Map<String, List<AppAsset>> {
        val result = mutableMapOf<String, List<AppAsset>>()
        try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)

            result["stickers"] = parseGroup(root, "stickers", AssetType.STICKER)
            result["frames"] = parseGroup(root, "frames", AssetType.FRAME)
            result["filters"] = parseGroup(root, "filters", AssetType.FILTER) // Thêm Filter ở đây

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun parseGroup(root: JSONObject, key: String, type: AssetType): List<AppAsset> {
        val list = mutableListOf<AppAsset>()
        root.optJSONArray(key)?.let { array ->
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(AppAsset(
                    id = obj.getInt("id"),
                    name = obj.getString("name"),
                    previewRes = getResId(obj.getString("preview_icon")),
                    // Với Filter, nếu không có res_name thì dùng chính ID làm mainRes
                    mainRes = if (obj.has("res_name")) getResId(obj.getString("res_name")) else obj.getInt("id"),
                    type = type
                ))
            }
        }
        return list
    }

    private fun getResId(resName: String): Int {
        if (resName == "none" || resName.isEmpty()) return -1
        return context.resources.getIdentifier(resName, "drawable", context.packageName)
    }
}