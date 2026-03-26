package com.sgroupmobile.glowza.ui.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.data.model.FunctionItem

class FunctionAdapter(
    private val items: List<FunctionItem>,
    private val onFunctionClicked: (FunctionItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_BIG = 1
        private const val TYPE_SMALL = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isBig) TYPE_BIG else TYPE_SMALL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_BIG) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_function_big, parent, false)
            BigVH(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_function_small, parent, false)
            SmallVH(view)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        if (holder is BigVH) {
            holder.bind(item)
        } else if (holder is SmallVH) {
            holder.bind(item)
        }
    }

    inner class BigVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: FunctionItem) {
            val context = itemView.context
            itemView.findViewById<TextView>(R.id.tv_title).text = context.getString(item.nameRes)
            itemView.findViewById<ImageView>(R.id.iv_icon).setImageResource(item.icon)
            itemView.setOnClickListener {
                onFunctionClicked(item)
            }
        }
    }

    inner class SmallVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: FunctionItem) {
            val context = itemView.context
            itemView.findViewById<TextView>(R.id.tv_title).text = context.getString(item.nameRes)
            itemView.findViewById<ImageView>(R.id.iv_icon).setImageResource(item.icon)
            itemView.setOnClickListener {
                onFunctionClicked(item)
            }
        }
    }
}