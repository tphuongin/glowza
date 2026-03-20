package com.sgroupmobile.glowza.ui.photo_editor.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.data.model.DisplayableItem
import com.sgroupmobile.glowza.databinding.ItemAssetGridBinding

class GenericGridAdapter(
    private val items: List<DisplayableItem>,
    private val onItemClick: (DisplayableItem) -> Unit
) : RecyclerView.Adapter<GenericGridAdapter.AssetViewHolder>() {

    inner class AssetViewHolder(val binding: ItemAssetGridBinding) : RecyclerView.ViewHolder(binding.root){
        fun onBind(position: Int){
            val item = items[position]
            if(item.imageBitmap != null ){
                binding.tvFilterName.visibility = View.VISIBLE
                binding.tvFilterName.text = item.displayName
                binding.root.setCardBackgroundColor(Color.TRANSPARENT)
            } else{
                binding.tvFilterName.visibility = View.GONE
                val color = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceVariant)
                binding.root.setCardBackgroundColor(color)
            }
            val dataToLoad: Any? = item.imageBitmap ?: item.imageRes

            Glide.with(binding.root)
                .load(dataToLoad)
                .placeholder(R.drawable.bg_filter_item_selected)
                .error(R.drawable.ic_crop)
                .into(binding.ivAssetThumb)

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val binding = ItemAssetGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AssetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun getItemCount() = items.size
}