package com.sgroupmobile.glowza.ui.gallery

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.common.enums.GalleryMode
import com.sgroupmobile.glowza.data.model.GalleryImage
import com.sgroupmobile.glowza.databinding.ItemGalleryBinding
class GalleryAdapter(
    private val mode: GalleryMode,
    private val onSingleClick: (GalleryImage) -> Unit,
    private val onMultiChange: (List<GalleryImage>) -> Unit
) : ListAdapter<GalleryImage, GalleryAdapter.ViewHolder>(DiffCallback) {

    private val selectedList = mutableListOf<GalleryImage>()

    class ViewHolder(val binding: ItemGalleryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        Glide.with(holder.itemView.context)
            .load(item.uri)
            .centerCrop()
            .thumbnail(0.1f)
            .placeholder(R.color.background)
            .into(holder.binding.ivPhoto)

        // UI selected
        holder.binding.viewOverlay.isVisible = item.isSelected
        holder.binding.imgCheck.isVisible = item.isSelected

        holder.itemView.setOnClickListener {
            if (mode == GalleryMode.SINGLE) {
                onSingleClick(item)
            } else {
                toggleSelection(item)
                notifyItemChanged(position)
                onMultiChange(selectedList)
            }
        }
    }

    private fun toggleSelection(item: GalleryImage) {
        item.isSelected = !item.isSelected

        if (item.isSelected) {
            if (selectedList.size >= 12) {
                item.isSelected = false
                return
            }
            selectedList.add(item)
        } else {
            selectedList.remove(item)
        }
    }

    fun getSelectedUris(): List<Uri> {
        return selectedList.map { it.uri }
    }

    object DiffCallback : DiffUtil.ItemCallback<GalleryImage>() {
        override fun areItemsTheSame(oldItem: GalleryImage, newItem: GalleryImage) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: GalleryImage, newItem: GalleryImage) = oldItem == newItem
    }
}