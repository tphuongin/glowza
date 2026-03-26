package com.sgroupmobile.glowza.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sgroupmobile.glowza.data.model.OnboardingItem
import com.sgroupmobile.glowza.databinding.ItemOnboardingBinding

class OnboardingAdapter(private val items: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(val binding: ItemOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        return OnboardingViewHolder(
            ItemOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvTitle.setText(item.titleRes)
            tvDescription.setText(item.descRes)
            lottie.setAnimation(item.imageRes)
            lottie.playAnimation()
        }
    }

    override fun getItemCount() = items.size
}