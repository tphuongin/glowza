package com.sgroupmobile.glowza.ui.photo_editor.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.data.model.CropRatio
import com.sgroupmobile.glowza.databinding.ItemCropRatioBinding

class CropOptionAdapter(
    private val items: List<CropRatio>,
    private val onItemSelected: (CropRatio) -> Unit
) : RecyclerView.Adapter<CropOptionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCropRatioBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCropRatioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvRatioName.text = item.name
        holder.binding.ivRatioIcon.setImageResource(item.icon)

        // Hiển thị màu hồng Pastel khi được chọn
        val context = holder.itemView.context
        val color = if (item.isSelected)
            ContextCompat.getColor(context, R.color.primary)
        else
            ContextCompat.getColor(context, R.color.onSurfaceVariant)

        holder.binding.ivRatioIcon.setColorFilter(color)
        holder.binding.tvRatioName.setTextColor(color)

        holder.itemView.setOnClickListener {
            items.forEach { it.isSelected = false }
            item.isSelected = true
            notifyDataSetChanged()
            onItemSelected(item)
        }
    }

    override fun getItemCount() = items.size
}