package com.sgroupmobile.glowza.ui.photo_editor.fragment


import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.databinding.LayoutSubToolDrawBinding
import com.sgroupmobile.glowza.ui.photo_editor.adapter.ColorAdapter

class DrawSubToolFragment : BaseFragment<LayoutSubToolDrawBinding>() {

    // Callbacks báo về Activity/View
    var onDrawConfigChanged: ((color: Int, size: Float, isEraser: Boolean) -> Unit)? = null
    var onUndoClicked: (() -> Unit)? = null
    var onClearClicked: (() -> Unit)? = null

    private var currentColor = Color.parseColor("#F48FB1")
    private var currentSize = 20f
    private var isEraser = false

    override fun provideBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutSubToolDrawBinding {
        return LayoutSubToolDrawBinding.inflate(inflater, container, false)
    }

    override fun initData() {
        // Khởi tạo các giá trị mặc định nếu cần
    }

    override fun setupUI() {
        // Thiết lập danh sách màu sắc
        val colors = listOf("#F48FB1", "#FFFFFF", "#000000", "#FFEB3B", "#4CAF50", "#2196F3", "#FF5722")
        binding.rvDrawColors.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = ColorAdapter(colors) { colorStr ->
                currentColor = Color.parseColor(colorStr)
                isEraser = false // Chọn màu thì tự động tắt chế độ xóa
                updateEraserUI()
                notifyChanges()
            }
        }

        // Cập nhật trạng thái UI ban đầu
        updateEraserUI()
    }

    override fun setupListeners() {
        // 1. Slider Kích thước bút
        binding.sliderBrushSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                currentSize = value
                notifyChanges()
            }
        }

        // 2. Nút Xóa (Eraser)
        binding.btnEraser.setOnClickListener {
            isEraser = !isEraser
            updateEraserUI()
            notifyChanges()
        }

        // 3. Nút Hoàn tác (Undo)
        binding.btnUndo.setOnClickListener {
            onUndoClicked?.invoke()
        }

        // 4. Nút Xóa hết (Clear All)
        binding.btnClearAll.setOnClickListener {
            onClearClicked?.invoke()
        }
    }

    private fun updateEraserUI() {
        // Hiển thị độ mờ để phân biệt nút đang bật hay tắt
        binding.btnEraser.alpha = if (isEraser) 1.0f else 0.4f
    }

    private fun notifyChanges() {
        onDrawConfigChanged?.invoke(currentColor, currentSize, isEraser)
    }
}