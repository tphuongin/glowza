package com.sgroupmobile.glowza.ui.photo_editor.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.data.model.TextItem

// 1. Adapter cho Màu sắc
class ColorAdapter(val colors: List<String>, val onClick: (String) -> Unit) :
    RecyclerView.Adapter<ColorAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v)
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_color_dot, p, false))
    override fun getItemCount() = colors.size
    override fun onBindViewHolder(h: VH, p: Int) {
        h.itemView.findViewById<View>(R.id.colorDot).setBackgroundColor(Color.parseColor(colors[p]))
        h.itemView.setOnClickListener { onClick(colors[p]) }
    }
}

// 2. Adapter cho Phông chữ
class FontAdapter(val fonts: List<String>, val onClick: (String) -> Unit) :
    RecyclerView.Adapter<FontAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v)
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_font_row, p, false))
    override fun getItemCount() = fonts.size
    override fun onBindViewHolder(h: VH, p: Int) {
        val tv = h.itemView.findViewById<TextView>(R.id.tvFontName)
        tv.text = fonts[p].removeSuffix(".ttf")
        // Preview font ngay trên list
        try {
            val tf = Typeface.createFromAsset(h.itemView.context.assets, "fonts/${fonts[p]}")
            tv.typeface = tf
        } catch (e: Exception) {}
        h.itemView.setOnClickListener { onClick(fonts[p]) }
    }
}

// 3. Adapter cho Mẫu (Templates)
class TemplateAdapter(val items: List<TextItem>, val onClick: (TextItem) -> Unit) :
    RecyclerView.Adapter<TemplateAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v)
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_text_template, p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, p: Int) {
        val tv = h.itemView.findViewById<TextView>(R.id.tvTemplatePreview)
        val item = items[p]
        tv.text = item.text
        tv.setTextColor(item.textPaint.color)
        tv.textSize = 18f
        h.itemView.setOnClickListener { onClick(item) }
    }
}