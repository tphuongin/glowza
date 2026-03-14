package com.sgroupmobile.glowza.util

import android.content.Context
import android.net.Uri

fun saveImageToGallery(context: Context, cacheUri: Uri, fileName: String): Uri? {
    val resolver = context.contentResolver

    //Cấu hình thông tin file cho MediaStore
    val imageDetails = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.jpg")
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        // Lưu vào thư mục Pictures/Glowza
        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Glowza")
    }

    // Chèn thông tin vào MediaStore để lấy một Uri "trống" trong Gallery
    val galleryUri = resolver.insert(
        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        imageDetails
    )

    return galleryUri?.also { uri ->
        try {
            //Mở luồng dữ liệu (Stream) để copy từ Cache sang Gallery
            resolver.openOutputStream(uri)?.use { outputStream ->
                context.contentResolver.openInputStream(cacheUri)?.use { inputStream ->
                    inputStream.copyTo(outputStream) // Copy dữ liệu
                }
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null) // Xóa Uri trống nếu lỗi
        }
    }
}
