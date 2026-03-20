package com.sgroupmobile.glowza.ui.photo_editor.fragment


import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.databinding.LayoutSubToolDrawBinding
import com.sgroupmobile.glowza.provider.ColorProvider
import com.sgroupmobile.glowza.ui.photo_editor.activity.EditorActivity
import com.sgroupmobile.glowza.ui.photo_editor.adapter.ColorAdapter

class DrawSubToolFragment : BaseFragment<LayoutSubToolDrawBinding>() {

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
        binding.rvDrawColors.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = ColorAdapter(colors) { colorStr ->
                currentColor = Color.parseColor(colorStr)
                isEraser = false
                updateEraserUI()
                notifyChanges()
            }
        }

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
            updateEraserUI()
            notifyChanges()
        }

        binding.btnUndo.setOnClickListener {
            onUndoClicked?.invoke()
        }

        binding.btnClearAll.setOnClickListener {
            onClearClicked?.invoke()
        }
        binding.btnConfirmDraw.setOnClickListener {
            (activity as? EditorActivity)?.confirmDrawAction()
        }
    }

    private fun updateEraserUI() {
        binding.btnEraser.alpha = if (isEraser) 1.0f else 0.4f
    }

    private fun notifyChanges() {
        onDrawConfigChanged?.invoke(currentColor, currentSize, isEraser)
    }
}