package com.sgroupmobile.glowza.helper

import jp.co.cyberagent.android.gpuimage.filter.*

object ImageFilterManager {

    fun getGPUFilter(code: String): GPUImageFilter {
        return when (code.uppercase()) {

            "GRAYSCALE" -> GPUImageGrayscaleFilter()

            "VINTAGE" -> GPUImageVignetteFilter().apply {
                // Tạo hiệu ứng cổ điển bằng cách làm tối 4 góc
                setVignetteStart(0.3f)
                setVignetteEnd(0.75f)
            }

            "PASTEL" -> GPUImageRGBFilter(1.1f, 0.9f, 1.0f).apply {
                // Chỉnh thông số RGB để tạo tông màu hồng phấn (Pastel)
                // Bạn có thể tùy chỉnh thêm Brightness để ảnh sáng hơn
            }

            "TOON" -> GPUImageToonFilter()

            "INVERT" -> GPUImageColorInvertFilter()

            // Nếu code là "NONE" hoặc không khớp, trả về filter mặc định (không đổi màu)
            else -> GPUImageFilter()
        }
    }
}