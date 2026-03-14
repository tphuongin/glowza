package com.sgroupmobile.glowza.ui.camera

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.data.model.FaceFilter
import com.sgroupmobile.glowza.databinding.ItemFilterBinding

class FilterAdapter(private val onFilterClicked: (FaceFilter) -> Unit) :
    ListAdapter<FaceFilter, FilterAdapter.ViewHolder>(DiffCallback()) {

    private var selectedPosition = 0

    inner class ViewHolder(private val binding: ItemFilterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(filter: FaceFilter, isSelected: Boolean) {
            binding.imgFilterPreview.setImageResource(filter.previewIcon)

            binding.imgFilterPreview.setBackgroundResource(
                if (isSelected) R.drawable.bg_filter_item_selected else R.drawable.bg_filter_item_unselected
            )

            val targetScale = if (isSelected) 1.25f else 1.0f
            val targetAlpha = if (isSelected) 1.0f else 0.7f

            binding.root.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .alpha(targetAlpha)
                .setDuration(250)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            binding.root.setOnClickListener {
                if (selectedPosition == adapterPosition) return@setOnClickListener

                val oldPos = selectedPosition
                selectedPosition = adapterPosition

                notifyItemChanged(oldPos)
                notifyItemChanged(selectedPosition)
                onFilterClicked(filter)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    class DiffCallback : DiffUtil.ItemCallback<FaceFilter>() {
        override fun areItemsTheSame(oldItem: FaceFilter, newItem: FaceFilter) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FaceFilter, newItem: FaceFilter) = oldItem == newItem
    }
}