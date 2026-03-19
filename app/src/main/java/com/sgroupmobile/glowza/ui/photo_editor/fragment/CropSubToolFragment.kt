package com.sgroupmobile.glowza.ui.photo_editor.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.data.model.CropRatio
import com.sgroupmobile.glowza.databinding.LayoutSubToolCropBinding
import com.sgroupmobile.glowza.ui.photo_editor.adapter.CropOptionAdapter

class CropSubToolFragment : BaseFragment<LayoutSubToolCropBinding>() {
    var onRatioSelected: ((Float) -> Unit)? = null

    override fun provideBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutSubToolCropBinding {
        return LayoutSubToolCropBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        binding.rvCropRatios.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    override fun initData() {
        val ratios = listOf(
            CropRatio(getString(R.string.crop_free), 0f, 0f, R.drawable.ic_crop, true),
            CropRatio("1:1", 1f, 1f, R.drawable.ic_crop),
            CropRatio("3:4", 3f, 4f, R.drawable.ic_crop),
            CropRatio("4:3", 4f, 3f, R.drawable.ic_crop),
            CropRatio("9:16", 9f, 16f, R.drawable.ic_crop)
        )

        // Gán adapter
        binding.rvCropRatios.adapter = CropOptionAdapter(ratios) { selected ->
            onRatioSelected?.invoke(selected.getRatioValue())
        }
    }

//    override fun setupListeners() {
//        // Nút xác nhận cắt
//        binding.btnApplyCrop.setOnClickListener {
//            // Ép kiểu activity về EditorActivity để gọi hàm xử lý cắt
//            (activity as? EditorActivity)?.applyCropAction()
//        }
//    }
}