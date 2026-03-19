package com.sgroupmobile.glowza.ui.photo_editor.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sgroupmobile.glowza.databinding.LayoutBottomSheetGridBinding
import com.sgroupmobile.glowza.helper.AssetHelper
import com.sgroupmobile.glowza.ui.photo_editor.adapter.GenericGridAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StickerBottomSheetFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var assetHelper: AssetHelper

    private var _binding: LayoutBottomSheetGridBinding? = null
    private val binding get() = _binding!!

    // Biến tạm để lưu callback
    var onItemSelected: ((Int) -> Unit)? = null

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_TYPE = "arg_type"

        // Dùng Static Factory để truyền data an toàn cho Fragment
        fun newInstance(title: String, type: String, callback: (Int) -> Unit): StickerBottomSheetFragment {
            return StickerBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_TYPE, type)
                }
                onItemSelected = callback
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutBottomSheetGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARG_TITLE) ?: ""
        val assetType = arguments?.getString(ARG_TYPE) ?: "stickers"

        binding.tvSheetTitle.text = title

        // --- BẮT ĐẦU LOG KIỂM TRA ---
        Log.d("GLOWZA_DEBUG", "Đang load asset type: $assetType")

        val allAssets = assetHelper.loadAssets("editor_assets.json")
        Log.d("GLOWZA_DEBUG", "Số lượng nhóm tìm thấy: ${allAssets.size}")

        val displayList = allAssets[assetType] ?: emptyList()
        Log.d("GLOWZA_DEBUG", "Số lượng item trong $assetType: ${displayList.size}")

        displayList.forEach {
            Log.d("GLOWZA_DEBUG", "Item: ${it.name}, PreviewRes: ${it.previewRes}, MainRes: ${it.mainRes}")
        }
        // --- KẾT THÚC LOG ---

        if (displayList.isEmpty()) {
            Log.e("GLOWZA_DEBUG", "CẢNH BÁO: Danh sách hiển thị trống! Kiểm tra lại file JSON hoặc AssetHelper.")
        }

        val spanCount = if (assetType == "stickers") 4 else 2
        binding.rvGrid.layoutManager = GridLayoutManager(requireContext(), spanCount)

        val adapter = GenericGridAdapter(displayList) { asset ->
            onItemSelected?.invoke(asset.mainRes)
            dismiss()
        }
        binding.rvGrid.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}