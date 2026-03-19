package com.sgroupmobile.glowza.ui.photo_editor.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.common.enums.AdjustType
import com.sgroupmobile.glowza.data.model.AdjustmentItem
import com.sgroupmobile.glowza.databinding.LayoutSubToolAdjustmentBinding
import com.sgroupmobile.glowza.ui.photo_editor.adapter.AdjustmentAdapter

class AdjustmentSubToolFragment : BaseFragment<LayoutSubToolAdjustmentBinding>() {

    // Khởi tạo bằng list rỗng để tránh crash UninitializedPropertyAccessException
    private var adjustItems: List<AdjustmentItem> = emptyList()
    private var currentActiveItem: AdjustmentItem? = null

    // Callback báo cho Activity để cập nhật EditorView
    var onAdjustmentChanged: ((brightness: Float, contrast: Float, saturation: Float) -> Unit)? = null

    override fun provideBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutSubToolAdjustmentBinding {
        return LayoutSubToolAdjustmentBinding.inflate(inflater, container, false)
    }

    override fun initData() {
        // Khởi tạo danh sách các thông số điều chỉnh
        adjustItems = listOf(
            AdjustmentItem(getString(R.string.adjust_brightness), AdjustType.BRIGHTNESS, R.drawable.ic_crop, 0f, -0.5f, 0.5f, 0f, true),
            AdjustmentItem(getString(R.string.adjust_contrast), AdjustType.CONTRAST, R.drawable.ic_crop, 1f, 0.5f, 1.5f, 1f),
            AdjustmentItem(getString(R.string.adjust_saturation), AdjustType.SATURATION, R.drawable.ic_crop, 1f, 0f, 2f, 1f)
        )

        // Mặc định chọn mục đầu tiên
        currentActiveItem = adjustItems[0]
    }

    override fun setupUI() {
        // Đảm bảo dữ liệu đã sẵn sàng trước khi setup UI
        if (adjustItems.isEmpty()) return

        // Thiết lập Slider ban đầu
        updateSlider()

        // Thiết lập RecyclerView
        binding.rvAdjustOptions.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = AdjustmentAdapter(adjustItems) { selected ->
                currentActiveItem = selected
                updateSlider()
            }
        }
    }

    override fun setupListeners() {
        // Lắng nghe sự kiện kéo Slider
        binding.sliderAdjust.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                currentActiveItem?.currentValue = value
                notifyChanges()
            }
        }
    }

    private fun updateSlider() {
        currentActiveItem?.let {
            // Lưu ý: Cần set valueFrom/valueTo trước khi set value để tránh crash nếu value nằm ngoài khoảng cũ
            binding.sliderAdjust.valueFrom = it.minValue
            binding.sliderAdjust.valueTo = it.maxValue
            binding.sliderAdjust.value = it.currentValue
        }
    }

    private fun notifyChanges() {
        if (adjustItems.isEmpty()) return

        val brightness = adjustItems.find { it.type == AdjustType.BRIGHTNESS }?.currentValue ?: 0f
        val contrast = adjustItems.find { it.type == AdjustType.CONTRAST }?.currentValue ?: 1f
        val saturation = adjustItems.find { it.type == AdjustType.SATURATION }?.currentValue ?: 1f
        onAdjustmentChanged?.invoke(brightness, contrast, saturation)
    }
}