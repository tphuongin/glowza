package com.sgroupmobile.glowza.ui.photo_editor.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sgroupmobile.glowza.common.enums.Constants.ARG_TITLE
import com.sgroupmobile.glowza.common.enums.Constants.ARG_TYPE
import com.sgroupmobile.glowza.databinding.LayoutBottomSheetGridBinding
import com.sgroupmobile.glowza.helper.AssetHelper
import com.sgroupmobile.glowza.ui.photo_editor.EditorActivity
import com.sgroupmobile.glowza.ui.photo_editor.EditorViewModel
import com.sgroupmobile.glowza.ui.photo_editor.adapter.GenericGridAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StickerBottomSheetFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var assetHelper: AssetHelper

    private var _binding: LayoutBottomSheetGridBinding? = null
    private val binding get() = _binding!!

    // Dùng chung ViewModel với Activity để quản lý preview
    private val viewModel: EditorViewModel by activityViewModels()

    var onItemSelected: ((Int) -> Unit)? = null

    companion object {
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

        // Thiết lập Grid hiển thị
        if (assetType == "filters") {
            setupFilterGrid()
        } else {
            setupAssetGrid(assetType)
        }

        setupControlButtons()
    }

    private fun setupControlButtons() {
        // Nút X: Hủy bỏ preview và đóng sheet
        binding.btnClose.setOnClickListener {
            (activity as? EditorActivity)?.cancelPendingAction()
            dismiss()
        }

        // Nút V: Xác nhận Action đang preview và đóng sheet
        binding.btnConfirm.setOnClickListener {
            (activity as? EditorActivity)?.confirmPendingAction()
            dismiss()
        }
    }

    private fun setupFilterGrid() {
        binding.rvGrid.layoutManager = GridLayoutManager(requireContext(), 4)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filterPreviews.collect { filters ->
                if (filters.isNotEmpty()) {
                    // Khi nhấn vào item: Chỉ gọi callback để Activity hiện preview
                    // KHÔNG gọi dismiss() ở đây
                    val adapter = GenericGridAdapter(filters) { filter ->
                        onItemSelected?.invoke(filter.id)
                    }
                    binding.rvGrid.adapter = adapter
                }
            }
        }
    }

    private fun setupAssetGrid(assetType: String) {
        val spanCount = if (assetType == "stickers") 4 else 2
        binding.rvGrid.layoutManager = GridLayoutManager(requireContext(), spanCount)

        val allAssets = assetHelper.loadAssets("editor_assets.json")
        val displayList = allAssets[assetType] ?: emptyList()

        // Tương tự Filter: Nhấn là xem thử, không đóng sheet
        val adapter = GenericGridAdapter(displayList) { asset ->
            if (asset.imageRes != null) {
                onItemSelected?.invoke(asset.imageRes!!)
            }
        }
        binding.rvGrid.adapter = adapter
    }

    override fun onDestroyView() {
        viewModel.clearFilterPreviews()
        super.onDestroyView()
        _binding = null
    }
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // Khi người dùng nhấn ra ngoài hoặc nhấn nút Back hệ thống để tắt Sheet
        // Chúng ta coi đó là hành động Hủy Preview
        (activity as? EditorActivity)?.cancelPendingAction()
    }
}