package com.sgroupmobile.glowza.provider

import android.content.res.AssetManager
import android.graphics.*
import com.sgroupmobile.glowza.data.model.TextItem

object TextTemplateProvider {
    fun getTemplates(assets: AssetManager): List<TextItem> {

        // Hàm hỗ trợ lấy Font nhanh
        fun getFont(path: String): Typeface {
            return try {
                Typeface.createFromAsset(assets, "fonts/$path")
            } catch (e: Exception) {
                Typeface.DEFAULT
            }
        }

        return listOf(
            TextItem("Retro 80s").apply {
                setTypeface(getFont("bold_retro.ttf"))
                setTextColor(Color.parseColor("#FFEB3B")) // Vàng
                setTextSize(100f)
                textPaint.setShadowLayer(10f, 8f, 8f, Color.parseColor("#E91E63")) // Đổ bóng hồng đậm
            },

            TextItem("Elegant").apply {
                setTypeface(getFont("beauty.ttf"))
                setTextColor(Color.parseColor("#D4AF37")) // Màu Gold
                setTextSize(90f)
                textPaint.letterSpacing = 0.2f
                textPaint.setShadowLayer(5f, 0f, 2f, Color.parseColor("#44000000"))
            },

            TextItem("Cyber").apply {
                setTypeface(getFont("modern.ttf"))
                setTextColor(Color.WHITE)
                setTextSize(110f)
                textPaint.style = Paint.Style.FILL_AND_STROKE
                textPaint.strokeWidth = 2f
                textPaint.setShadowLayer(25f, 0f, 0f, Color.parseColor("#00E5FF"))
            },

            TextItem("Journal").apply {
                setTypeface(getFont("classic.ttf"))
                setTextColor(Color.parseColor("#2C3E50"))
                setTextSize(95f)
                textPaint.isFakeBoldText = true
            },

            TextItem("Lovely").apply {
                setTypeface(getFont("beauty.ttf"))
                setTextColor(Color.WHITE)
                setTextSize(100f)
                textPaint.setShadowLayer(15f, 0f, 5f, Color.parseColor("#F48FB1"))
            },

            TextItem("Minimal").apply {
                setTypeface(getFont("standard.ttf"))
                setTextColor(Color.parseColor("#333333"))
                setTextSize(80f)
                textPaint.letterSpacing = 0.3f
            },

            TextItem("Glowza").apply {
                setTypeface(getFont("bold_retro.ttf"))
                setTextSize(110f)
                val gradient = LinearGradient(0f, 0f, 0f, 100f,
                    Color.parseColor("#9C27B0"),
                    Color.parseColor("#E91E63"),
                    Shader.TileMode.CLAMP)
                textPaint.shader = gradient
                textPaint.setShadowLayer(10f, 0f, 5f, Color.parseColor("#44000000"))
            }
        )
    }

    fun getFontList() = listOf("standard.ttf", "beauty.ttf", "bold_retro.ttf", "classic.ttf", "modern.ttf")

}