package com.sgroupmobile.glowza.ui.photo_editor.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.data.model.TextItem


class ColorAdapter(
    private val colors: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorVH>() {

    private var selectedPosition = 0
    class ColorVH(v: View) : RecyclerView.ViewHolder(v) {
        val cardColor: CardView = v.findViewById(R.id.cardColor)
        val viewSelected: View = v.findViewById(R.id.viewSelected)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): ColorVH {
        val view = LayoutInflater.from(p.context).inflate(R.layout.item_color_dot, p, false)
        return ColorVH(view)
    }

    override fun getItemCount() = colors.size

    override fun onBindViewHolder(h: ColorVH, position: Int) {
        val colorCode = colors[position]
        try {
            h.cardColor.setCardBackgroundColor(Color.parseColor(colorCode))
        } catch (e: Exception) {
            h.cardColor.setCardBackgroundColor(Color.WHITE)
        }
        h.viewSelected.isVisible = (position == selectedPosition)

        h.itemView.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = h.adapterPosition

            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)

            onClick(colorCode)
        }
    }
}

class FontAdapter(private val fonts: List<String>, private val onClick: (String) -> Unit) :
    RecyclerView.Adapter<FontAdapter.FontVH>() {
    private val fontCache = mutableMapOf<String, Typeface>()
    class FontVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvFont: TextView = v.findViewById(R.id.tvFontName)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        FontVH(LayoutInflater.from(p.context).inflate(R.layout.item_font_row, p, false))

    override fun getItemCount() = fonts.size

    override fun onBindViewHolder(h: FontVH, p: Int) {
        val fontPath = fonts[p]
        h.tvFont.text = fontPath.removeSuffix(".ttf").replaceFirstChar { it.uppercase() }

        val tf = fontCache.getOrPut(fontPath) {
            try {
                Typeface.createFromAsset(h.itemView.context.assets, "fonts/$fontPath")
            } catch (e: Exception) {
                Typeface.DEFAULT
            }
        }
        h.tvFont.typeface = tf
        h.itemView.setOnClickListener { onClick(fontPath) }
    }
}

class TemplateAdapter(private val items: List<TextItem>, private val onClick: (TextItem) -> Unit) :
    RecyclerView.Adapter<TemplateAdapter.TemplateVH>() {

    class TemplateVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvPreview: TextView = v.findViewById(R.id.tvTemplatePreview)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        TemplateVH(LayoutInflater.from(p.context).inflate(R.layout.item_text_template, p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: TemplateVH, p: Int) {
        val item = items[p]
        val tv = h.tvPreview
        val paint = item.textPaint

        tv.text = item.text
        tv.typeface = paint.typeface
        tv.setTextColor(paint.color)
        tv.textSize = 18f

        tv.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        tv.setShadowLayer(
            paint.shadowLayerRadius,
            paint.shadowLayerDx,
            paint.shadowLayerDy,
            paint.shadowLayerColor
        )

        tv.paint.shader = paint.shader

        tv.letterSpacing = paint.letterSpacing

        h.itemView.setOnClickListener { onClick(item) }
    }
}