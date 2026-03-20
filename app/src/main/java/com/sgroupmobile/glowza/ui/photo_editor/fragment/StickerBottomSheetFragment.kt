package com.sgroupmobile.glowza.ui.photo_editor.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
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

        // CHỈ ẨN NÚT NẾU LÀ STICKER
        if (assetType == "stickers") {
            binding.btnClose.visibility = View.GONE
            binding.btnConfirm.visibility = View.GONE
        }

        if (assetType == "filters") {
            setupFilterGrid()
        } else {
            setupAssetGrid(assetType)
        }

        setupControlButtons()
    }

    private fun setupControlButtons() {
        binding.btnClose.setOnClickListener {
            (activity as? EditorActivity)?.cancelPendingAction()
            dismiss()
        }

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
                    val adapter = GenericGridAdapter(filters) { filter ->
                        // Filter vẫn chỉ Preview, chờ nhấn V mới Confirm
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

        val adapter = GenericGridAdapter(displayList) { asset ->
            asset.imageRes?.let { resId ->
                onItemSelected?.invoke(resId)

                // NẾU LÀ STICKER: Confirm và đóng ngay lập tức
                if (assetType == "stickers") {
                    (activity as? EditorActivity)?.confirmPendingAction()
                    dismiss()
                }
                // NẾU LÀ FRAME: Chỉ hiện preview (vì nút V vẫn hiện để người dùng nhấn)
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
        // Nếu thoát bằng cách nhấn ra ngoài mà không nhấn V, hãy hủy preview (cho Filter/Frame)
        (activity as? EditorActivity)?.cancelPendingAction()
    }
}