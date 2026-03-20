package com.sgroupmobile.glowza.provider


object ColorProvider {
    fun getEditorColors(): List<String> {
        return listOf(
            // --- 1. Basic & Grayscale ---
            "#FFFFFF","#000000",

            // --- 2. Glowza Pastel (Đúng gu của Phương) ---
            "#F48FB1", // Hồng Pastel
            "#F8BBD0", // Hồng nhạt
            "#CE93D8", // Tím nhạt
            "#E1BEE7", // Tím Lavender
            "#90CAF9", // Xanh dương Pastel
            "#B3E5FC", // Xanh Sky
            "#A5D6A7", // Xanh lá bơ
            "#C8E6C9", // Xanh bạc hà
            "#FFF59D", // Vàng chanh nhạt
            "#FFE0B2", // Cam sữa
            "#EEEEEE", "#BDBDBD", "#757575", "#424242",

            // --- 3. Vibrant & Modern (Màu đậm, nổi bật) ---
            "#E91E63", // Hồng đậm
            "#9C27B0", // Tím đậm
            "#2196F3", // Xanh Blue
            "#00BCD4", // Xanh Cyan
            "#4CAF50", // Xanh Green
            "#FFEB3B", // Vàng rực
            "#FF9800", // Cam
            "#F44336", // Đỏ
            "#673AB7", // Deep Purple

            // --- 4. Vintage & Earthy (Hoài cổ, trầm) ---
            "#795548", // Nâu đất
            "#A1887F", // Nâu nhạt
            "#607D8B", // Xanh xám
            "#546E7A", // Xanh rêu tối
            "#3E2723", // Nâu đen
            "#5D4037", // Cafe

            // --- 5. Neon & Cyber (Cực sáng) ---
            "#00FF00", // Lime Neon
            "#00FFFF", // Aqua Neon
            "#FF00FF", // Fuchsia
            "#FFFF00"  // Neon Yellow
        )
    }
}