package com.sgroupmobile.glowza.ui.gallery

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.data.model.GalleryImage
import com.sgroupmobile.glowza.databinding.ItemGalleryBinding

class GalleryAdapter(private val onClick: (GalleryImage) -> Unit) :
    ListAdapter<GalleryImage, GalleryAdapter.ViewHolder>(DiffCallback) {

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

        holder.itemView.setOnClickListener { onClick(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<GalleryImage>() {
        override fun areItemsTheSame(oldItem: GalleryImage, newItem: GalleryImage) = oldItem.id == newItem.id
        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: GalleryImage, newItem: GalleryImage) = oldItem == newItem
    }
}