package com.sgroupmobile.glowza.ui.photo_editor.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.common.enums.AdjustType
import com.sgroupmobile.glowza.data.model.AdjustmentItem
import com.sgroupmobile.glowza.databinding.LayoutSubToolAdjustmentBinding
import com.sgroupmobile.glowza.ui.photo_editor.activity.EditorActivity
import com.sgroupmobile.glowza.ui.photo_editor.adapter.AdjustmentAdapter

class AdjustmentSubToolFragment : BaseFragment<LayoutSubToolAdjustmentBinding>() {
    private var adjustItems: List<AdjustmentItem> = emptyList()
    private var currentActiveItem: AdjustmentItem? = null
    private var isConfirmed = false

    var onAdjustmentChanged: ((brightness: Float, contrast: Float, saturation: Float) -> Unit)? = null

    override fun provideBinding(inflater: LayoutInflater, container: ViewGroup?): LayoutSubToolAdjustmentBinding {
        return LayoutSubToolAdjustmentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initData() // Nạp data trước
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
    }

    override fun initData() {
        adjustItems = listOf(
            AdjustmentItem(getString(R.string.adjust_brightness), AdjustType.BRIGHTNESS, R.drawable.ic_brightness, 0f, -0.5f, 0.5f, 0f, true),
            AdjustmentItem(getString(R.string.adjust_contrast), AdjustType.CONTRAST, R.drawable.ic_contrast, 1f, 0.5f, 1.5f, 1f),
            AdjustmentItem(getString(R.string.adjust_saturation), AdjustType.SATURATION, R.drawable.ic_saturation, 1f, 0f, 2f, 1f)
        )
        currentActiveItem = adjustItems[0]
    }

    override fun setupUI() {
        if (adjustItems.isEmpty()) return
        updateSlider()

        binding.rvAdjustOptions.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = AdjustmentAdapter(adjustItems) { selected ->
                currentActiveItem = selected
                updateSlider()
            }
        }
    }

    override fun setupListeners() {
        binding.sliderAdjust.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                currentActiveItem?.currentValue = value
                notifyChanges()
            }
        }

        binding.btnCancelAdjust.setOnClickListener {
            it.isEnabled = false
            val act = (activity as? EditorActivity)
            act?.cancelPendingAction()
            act?.closeSubTool()
            it.isEnabled = true
            Log.e("eeeeeeee", "cancel click ${act}")
        }

        binding.btnConfirmAdjust.setOnClickListener {
            it.isEnabled = false // Vô hiệu hóa nút ngay lập tức
            isConfirmed = true
            val act = (activity as? EditorActivity)
            act?.confirmPendingAction()
            act?.closeSubTool()
            it.isEnabled = true
            Log.e("eeeeeeee", "confirm click ${act}")
        }
    }

    override fun onDestroyView() {
        val act = (activity as? EditorActivity)
        if (!isConfirmed && act?.getPendingAction() != null) {
            act.cancelPendingAction()
        }
        super.onDestroyView()
    }
    private fun updateSlider() {
        currentActiveItem?.let {
            binding.sliderAdjust.valueFrom = it.minValue
            binding.sliderAdjust.valueTo = it.maxValue
            binding.sliderAdjust.value = it.currentValue
        }
    }

    private fun notifyChanges() {
        val b = adjustItems.find { it.type == AdjustType.BRIGHTNESS }?.currentValue ?: 0f
        val c = adjustItems.find { it.type == AdjustType.CONTRAST }?.currentValue ?: 1f
        val s = adjustItems.find { it.type == AdjustType.SATURATION }?.currentValue ?: 1f
        onAdjustmentChanged?.invoke(b, c, s)
    }
}