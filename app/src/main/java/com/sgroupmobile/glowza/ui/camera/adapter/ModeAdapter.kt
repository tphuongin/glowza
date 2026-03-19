package com.sgroupmobile.glowza.ui.camera.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.common.enums.CameraMode
import com.sgroupmobile.glowza.databinding.ItemModeBinding
import androidx.core.graphics.toColorInt

class ModeAdapter(
    private val modes: List<CameraMode>,
    private val onItemClick: (Int) -> Unit
):
    RecyclerView.Adapter<ModeAdapter.ViewHolder>() {
    private var selectedPosition = 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ModeAdapter.ViewHolder {
        val binding = ItemModeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModeAdapter.ViewHolder, position: Int) {
        holder.onBind(position)
    }


    override fun getItemCount(): Int = modes.size

    inner class ViewHolder(val binding: ItemModeBinding): RecyclerView.ViewHolder(binding.root){
        fun onBind(position: Int){
            binding.tvMode.text = modes[position].name
            if(position == selectedPosition){
                binding.tvMode.setTextColor("#E6B4DA".toColorInt())
            } else{
                binding.tvMode.setTextColor(Color.WHITE)
            }
            itemView.setOnClickListener {
                onItemClick(position)
            }
        }
    }

    fun updateSelectedPosition(newPosition: Int){
        val oldPosition = selectedPosition
        if(newPosition >= 0 && newPosition < modes.size){
            selectedPosition = newPosition
        }
        notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }
}