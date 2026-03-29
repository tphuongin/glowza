package com.sgroupmobile.glowza.ui.photo_editor.fragment


import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.databinding.LayoutSubToolDrawBinding
import com.sgroupmobile.glowza.provider.ColorProvider
import com.sgroupmobile.glowza.ui.photo_editor.activity.EditorActivity
import com.sgroupmobile.glowza.ui.photo_editor.adapter.ColorAdapter

class DrawSubToolFragment : BaseFragment<LayoutSubToolDrawBinding>() {

    private var colorAdapter: ColorAdapter? = null
    var onDrawConfigChanged: ((color: Int, size: Float, isEraser: Boolean) -> Unit)? = null
    var onUndoClicked: (() -> Unit)? = null
    var onClearClicked: (() -> Unit)? = null

    var onRedoClicked: (() -> Unit)? = null
    private var currentColor = Color.parseColor("#F48FB1")
    private var currentSize = 20f
    private var isEraser = false

    override fun provideBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutSubToolDrawBinding {
        return LayoutSubToolDrawBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        val colors = ColorProvider.getEditorColors()
        colorAdapter = ColorAdapter(colors) { colorStr ->
            currentColor = Color.parseColor(colorStr)
            isEraser = false // Khi chọn màu thì tắt chế độ tẩy
            updateEraserUI()
            notifyChanges()
        }

        binding.rvDrawColors.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = colorAdapter
        }

        updateUndoRedoUI()
        updateEraserUI()
    }

    override fun setupListeners() {
        binding.sliderBrushSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                currentSize = value
                notifyChanges()
            }
        }
        binding.btnRedo.setOnClickListener {
            onRedoClicked?.invoke()
        }

        binding.btnEraser.setOnClickListener {
            isEraser = !isEraser
            if (isEraser) {
                // Nếu bật tẩy, reset dấu chọn bên danh sách màu
                colorAdapter?.clearSelection()
            }
            updateEraserUI()
            notifyChanges()
        }

        binding.btnUndo.setOnClickListener {
            onUndoClicked?.invoke()
        }

        binding.btnCancelDraw.setOnClickListener {
            onClearClicked?.invoke()
            it.isEnabled = false
            val act = (activity as? EditorActivity)
            act?.cancelPendingAction()
            act?.closeSubTool()
            it.isEnabled = true
        }
        binding.btnConfirmDraw.setOnClickListener {
            (activity as? EditorActivity)?.confirmDrawAction()
        }
    }

    private fun updateEraserUI() {
        if (isEraser) {
            // Khi focus: Hiện nền (ví dụ dùng màu primaryContainer mờ hoặc tint đậm)
            binding.btnEraser.setBackgroundResource(R.drawable.bg_circle_selected) // Bạn tạo drawable này nhé
            binding.btnEraser.alpha = 1.0f
        } else {
            // Khi mất focus: Chỉ hiện icon mờ, không nền
            binding.btnEraser.background = null
            binding.btnEraser.alpha = 0.5f
        }
    }
    fun updateUndoRedoUI(canUndo: Boolean = false, canRedo: Boolean = false) {
        binding.btnUndo.apply {
            isEnabled = canUndo
            alpha = if (canUndo) 1.0f else 0.3f
        }
        binding.btnRedo.apply {
            isEnabled = canRedo
            alpha = if (canRedo) 1.0f else 0.3f
        }
    }

    private fun notifyChanges() {
        onDrawConfigChanged?.invoke(currentColor, currentSize, isEraser)
    }
}