package com.sgroupmobile.glowza.ui.photo_editor.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.data.model.TextItem
import com.sgroupmobile.glowza.databinding.ItemFontBinding


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
    @SuppressLint("NotifyDataSetChanged")
    fun resetSelection() {
        selectedPosition = 0
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(h: ColorVH, position: Int) {
        val colorCode = colors[position]
        try {
            h.cardColor.setCardBackgroundColor(Color.parseColor(colorCode))
        } catch (e: Exception) {
            h.cardColor.setCardBackgroundColor(Color.WHITE)
        }
        h.viewSelected.isVisible = (position == selectedPosition)

        h.itemView.setOnClickListener {
            if (selectedPosition != h.adapterPosition) {
                val oldPosition = selectedPosition
                selectedPosition = h.adapterPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onClick(colorCode)
            }
        }
    }
}
class FontAdapter(
    private val fonts: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<FontAdapter.FontViewHolder>() {

    private var selectedPosition = -1

    inner class FontViewHolder(val binding: ItemFontBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(fontPath: String, position: Int) {
            val context = itemView.context

            val fontName = fontPath.replace(".ttf", "")
                .split("_")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
            binding.tvFontName.text = fontName
            try {
                val typeface = Typeface.createFromAsset(context.assets, "fonts/$fontPath")
                binding.tvFontPreview.typeface = typeface
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (position == selectedPosition) {
                binding.root.strokeColor = ContextCompat.getColor(context, R.color.primary)
                binding.root.strokeWidth = 4 // Đơn vị px (~1.5dp)
                binding.tvFontName.setTextColor(ContextCompat.getColor(context, R.color.primary))
            } else {
                // Trạng thái bình thường: Viền xám nhạt 1dp
                binding.root.strokeColor = Color.parseColor("#E0E0E0")
                binding.root.strokeWidth = 2
                binding.tvFontName.setTextColor(Color.parseColor("#888888"))
            }

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition

                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                onItemClick(fontPath)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val binding = ItemFontBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(fonts[position], position)
    }

    override fun getItemCount(): Int = fonts.size

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedFont(fontPath: String) {
        val index = fonts.indexOf(fontPath)
        if (index != -1 && index != selectedPosition) {
            selectedPosition = index
            notifyDataSetChanged()
        }
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

    @RequiresApi(Build.VERSION_CODES.Q)
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