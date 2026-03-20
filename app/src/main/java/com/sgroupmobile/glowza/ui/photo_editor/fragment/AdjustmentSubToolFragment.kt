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
import com.sgroupmobile.glowza.ui.photo_editor.EditorActivity
import com.sgroupmobile.glowza.ui.photo_editor.EditorViewModel
import com.sgroupmobile.glowza.ui.photo_editor.adapter.AdjustmentAdapter
import kotlin.getValue

class AdjustmentSubToolFragment : BaseFragment<LayoutSubToolAdjustmentBinding>() {
    private val viewModel: EditorViewModel by activityViewModels()


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
        Log.d("Glowza_Debug", "Fragment đã sẵn sàng với ${adjustItems.size} items")
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
            // Dùng 3 cột để căn giữa 3 icon tuyệt đối
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = AdjustmentAdapter(adjustItems) { selected ->
                currentActiveItem = selected
                updateSlider()
            }
        }
    }

    // Trong AdjustmentSubToolFragment.kt

    override fun setupListeners() {
        binding.sliderAdjust.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                currentActiveItem?.currentValue = value
                notifyChanges()
            }
        }

        binding.btnCancelAdjust.setOnClickListener {
            it.isEnabled = false // Vô hiệu hóa nút ngay lập tức để tránh click đúp
            val act = (activity as? EditorActivity)
            act?.cancelPendingAction()
            act?.closeSubTool()
        }

        binding.btnConfirmAdjust.setOnClickListener {
            it.isEnabled = false // Vô hiệu hóa nút ngay lập tức
            isConfirmed = true
            val act = (activity as? EditorActivity)
            act?.confirmPendingAction()
            act?.closeSubTool()
        }
    }

    override fun onDestroyView() {
        val act = (activity as? EditorActivity)
        Log.d("Glowza_Logic", "Fragment: onDestroyView chạy. isConfirmed = $isConfirmed, pending = ${act?.getPendingAction()}")

        if (!isConfirmed && act?.getPendingAction() != null) {
            Log.d("Glowza_Logic", "Fragment: Tự động hủy preview vì thoát tool mà chưa nhấn V")
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