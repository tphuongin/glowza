package com.sgroupmobile.glowza.helper

import android.content.Context
import android.graphics.Bitmap
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

            // Parse các nhóm Sticker và Frame từ JSON
            result["stickers"] = parseGroup(root, "stickers", AssetType.STICKER)
            result["frames"] = parseGroup(root, "frames", AssetType.FRAME)

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

                // Lấy resource ID từ tên string trong JSON
                val previewId = getResId(obj.getString("res_name"))
                val mainId = if (obj.has("res_name")) getResId(obj.getString("res_name")) else obj.getInt("id")

                list.add(AppAsset(
                    id = obj.getInt("id"),
                    displayName = obj.getString("name"),
                    mainRes = previewId,
                    type = type
                ).apply {
                    // CẬP NHẬT CHO INTERFACE:
                    // Sticker/Frame dùng Resource ID để hiển thị, không dùng Bitmap
                    this.imageRes = previewId
                    this.imageBitmap = null
                    this.displayName = obj.getString("name")
                })
            }
        }
        return list
    }

    private fun getResId(resName: String): Int {
        if (resName == "none" || resName.isEmpty()) return -1
        // Trả về 0 nếu không tìm thấy resource thay vì crash app
        return context.resources.getIdentifier(resName, "drawable", context.packageName)
    }
}