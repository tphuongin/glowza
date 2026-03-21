package com.sgroupmobile.glowza.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.sgroupmobile.glowza.databinding.ItemImageBinding

class ImageAdapter(private val list: List<Int>) :
    RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context

        Glide.with(context)
            .load(list[position])
            .transform(CenterCrop(), RoundedCorners(24))
            .into(holder.binding.img)
    }
}