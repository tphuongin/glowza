package com.sgroupmobile.glowza.ui.photo_editor.adapter

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
        val binding = ItemAdjustmentOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvAdjustName.text = item.name
        holder.binding.ivAdjustIcon.setImageResource(item.icon)

        val context = holder.itemView.context
        val color = if (item.isSelected)
            ContextCompat.getColor(context, R.color.primary)
        else
            ContextCompat.getColor(context, R.color.onSurfaceVariant)

        holder.binding.ivAdjustIcon.setColorFilter(color)
        holder.binding.tvAdjustName.setTextColor(color)

        holder.itemView.setOnClickListener {
            if (item.isSelected) return@setOnClickListener

            items.forEach { it.isSelected = false }
            item.isSelected = true
            notifyDataSetChanged()
            onItemSelected(item)
        }
    }

    override fun getItemCount() = items.size
}