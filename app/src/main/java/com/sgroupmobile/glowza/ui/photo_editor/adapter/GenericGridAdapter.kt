package com.sgroupmobile.glowza.ui.photo_editor.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.data.model.AppAsset
import com.sgroupmobile.glowza.databinding.ItemAssetGridBinding

class GenericGridAdapter(
    private val items: List<AppAsset>,
    private val onItemClick: (AppAsset) -> Unit
) : RecyclerView.Adapter<GenericGridAdapter.AssetViewHolder>() {

    inner class AssetViewHolder(val binding: ItemAssetGridBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val binding = ItemAssetGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AssetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        val item = items[position]

        // Log để kiểm tra từng item khi vẽ
        Log.d("GLOWZA_CHECK", "Binding item: ${item.name} với ID: ${item.previewRes}")

        Glide.with(holder.itemView.context)
            .load(item.previewRes)
            .placeholder(R.drawable.bg_filter_item_selected)
            .error(R.drawable.ic_crop) // Nếu lỗi ảnh nó sẽ hiện icon Crop để bạn biết
            .into(holder.binding.ivAssetThumb)

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size
}