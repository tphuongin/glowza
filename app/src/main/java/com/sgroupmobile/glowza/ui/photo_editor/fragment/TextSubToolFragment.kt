package com.sgroupmobile.glowza.ui.photo_editor.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.sgroupmobile.glowza.databinding.LayoutSubToolTextBinding
import com.sgroupmobile.glowza.ui.photo_editor.TextItem
import com.sgroupmobile.glowza.ui.photo_editor.adapter.ColorAdapter
import com.sgroupmobile.glowza.ui.photo_editor.adapter.FontAdapter
import com.sgroupmobile.glowza.ui.photo_editor.adapter.TemplateAdapter

class TextSubToolFragment : Fragment() {
    private var _binding: LayoutSubToolTextBinding? = null
    private val binding get() = _binding!!

    // Các Callback báo về Activity
    var onStyleUpdated: (() -> Unit)? = null
    var onTemplateSelected: ((TextItem) -> Unit)? = null

    private var activeTextItem: TextItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutSubToolTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupStyleListeners()
        setupLists()
    }

    /**
     * Hàm quan trọng để Activity truyền vật thể đang chọn vào Fragment
     */
    fun setTargetItem(item: TextItem) {
        this.activeTextItem = item
        // Cập nhật giá trị Slider theo Item hiện tại nếu View đã khởi tạo
        _binding?.let {
            it.sliderSize.value = item.textPaint.textSize.coerceIn(10f, 200f)
            it.sliderAlpha.value = item.textPaint.alpha.toFloat().coerceIn(0f, 255f)
        }
    }

    private fun setupTabs() {
        binding.tabLayoutText.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.rvTemplates.isVisible = tab?.position == 0
                binding.layoutStyle.isVisible = tab?.position == 1
                binding.rvFonts.isVisible = tab?.position == 2
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupStyleListeners() {
        // 1. Slider Kích thước
        binding.sliderSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                activeTextItem?.setTextSize(value)
                onStyleUpdated?.invoke()
            }
        }

        // 2. Slider Độ mờ (Opacity)
        binding.sliderAlpha.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                activeTextItem?.setTextAlpha(value.toInt())
                onStyleUpdated?.invoke()
            }
        }
    }

    private fun setupLists() {
        // 3. RecyclerView Màu sắc
        val colors = listOf(
            "#FFFFFF", "#F48FB1", "#CE93D8", "#90CAF9",
            "#000000", "#FFEB3B", "#4CAF50", "#FF5722",
            "#795548", "#607D8B", "#9E9E9E", "#FFC107"
        )
        binding.rvColors.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvColors.adapter = ColorAdapter(colors) { colorStr ->
            activeTextItem?.setTextColor(Color.parseColor(colorStr))
            onStyleUpdated?.invoke()
        }

        // 4. RecyclerView Phông chữ
        // Lưu ý: Đảm bảo các file .ttf nằm trong assets/fonts/
        val fonts = listOf("standard.ttf", "beauty.ttf", "bold_retro.ttf", "classic.ttf", "modern.ttf")
        binding.rvFonts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFonts.adapter = FontAdapter(fonts) { fontPath ->
            try {
                val tf = Typeface.createFromAsset(requireContext().assets, "fonts/$fontPath")
                activeTextItem?.setTypeface(tf)
                onStyleUpdated?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 5. RecyclerView Mẫu (Templates)
        val templates = listOf(
            TextItem("Pastel").apply { setTextColor(Color.parseColor("#F48FB1")); setTextSize(50f) },
            TextItem("Neon").apply { setTextColor(Color.CYAN); setTextSize(50f) },
            TextItem("Shadow").apply { setTextColor(Color.BLACK); setTextSize(50f) },
            TextItem("Soft").apply { setTextColor(Color.parseColor("#CE93D8")); setTextSize(50f) }
        )
        binding.rvTemplates.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvTemplates.adapter = TemplateAdapter(templates) { template ->
            onTemplateSelected?.invoke(template)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}