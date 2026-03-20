package com.sgroupmobile.glowza.extension

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.InputStream


fun Uri.toBitmap(context: Context): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(this)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}