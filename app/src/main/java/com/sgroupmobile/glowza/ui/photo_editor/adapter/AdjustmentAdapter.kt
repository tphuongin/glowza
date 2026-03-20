package com.sgroupmobile.glowza.ui.photo_editor.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.data.model.AdjustmentItem
import com.sgroupmobile.glowza.databinding.ItemAdjustmentOptionBinding

class AdjustmentAdapter(
    private val items: List<AdjustmentItem>,
    private val onItemSelected: (AdjustmentItem) -> Unit
) : RecyclerView.Adapter<AdjustmentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdjustmentOptionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("Glowza_Debug", "Adapter: Đang tạo ViewHolder")
        val binding = ItemAdjustmentOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.binding.apply {
            // Đồng bộ với ID snake_case trong XML: tv_adjust_name, iv_adjust_icon
            tvAdjustName.text = item.name
            ivAdjustIcon.setImageResource(item.icon)

            val color = if (item.isSelected)
                ContextCompat.getColor(context, R.color.primary)
            else
                ContextCompat.getColor(context, R.color.onSurfaceVariant)

            ivAdjustIcon.setColorFilter(color)
            tvAdjustName.setTextColor(color)

            root.setOnClickListener {
                if (item.isSelected) return@setOnClickListener
                items.forEach { it.isSelected = false }
                item.isSelected = true
                notifyDataSetChanged()
                onItemSelected(item)
            }
        }
    }

    override fun getItemCount() = items.size
}