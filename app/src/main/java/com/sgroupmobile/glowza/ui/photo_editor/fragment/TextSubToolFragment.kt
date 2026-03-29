package com.sgroupmobile.glowza.ui.photo_editor.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.databinding.LayoutSubToolTextBinding
import com.sgroupmobile.glowza.data.model.TextItem
import com.sgroupmobile.glowza.provider.ColorProvider
import com.sgroupmobile.glowza.provider.TextTemplateProvider
import com.sgroupmobile.glowza.ui.photo_editor.adapter.ColorAdapter
import com.sgroupmobile.glowza.ui.photo_editor.adapter.FontAdapter
import com.sgroupmobile.glowza.ui.photo_editor.adapter.TemplateAdapter

class TextSubToolFragment : BaseFragment<LayoutSubToolTextBinding>() {
    private var colorAdapter: ColorAdapter? = null
    var onStyleUpdated: (() -> Unit)? = null
    var onTemplateSelected: ((TextItem) -> Unit)? = null

    private var activeTextItem: TextItem? = null

    override fun provideBinding(inflater: LayoutInflater, container: ViewGroup?) =
        LayoutSubToolTextBinding.inflate(inflater, container, false)

    override fun setupUI() {
        setupTabs()
        setupLists()

        activeTextItem?.let { item ->
            binding.sliderSize.value = item.textPaint.textSize.coerceIn(10f, 200f)
            binding.sliderAlpha.value = item.textPaint.alpha.toFloat().coerceIn(0f, 255f)
        }
    }

    override fun setupListeners() {
        setupStyleListeners()

        binding.btnResetColor.setOnClickListener {
            activeTextItem?.let { item ->
                item.textPaint.shader = null
                item.setTextColor(Color.WHITE)

                colorAdapter?.resetSelection()

                onStyleUpdated?.invoke()
            }
        }
    }

    fun setTargetItem(item: TextItem) {
        this.activeTextItem = item
        if (view != null) {
            // Tạm thời gỡ bỏ listener để tránh trigger nhầm khi tự động set value
            binding.sliderSize.clearOnChangeListeners()
            binding.sliderAlpha.clearOnChangeListeners()

            // Cập nhật giá trị thanh trượt theo chữ mới
            binding.sliderSize.value = item.textPaint.textSize.coerceIn(10f, 200f)
            binding.sliderAlpha.value = item.textPaint.alpha.toFloat().coerceIn(0f, 255f)

            // Gắn lại listener
            setupStyleListeners()
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
        binding.sliderSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                activeTextItem?.setTextSize(value)
                onStyleUpdated?.invoke()
            }
        }

        binding.sliderAlpha.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                activeTextItem?.setTextAlpha(value.toInt())
                onStyleUpdated?.invoke()
            }
        }
    }

    private fun setupLists() {
        // Màu sắc
        val colors = ColorProvider.getEditorColors()
        binding.rvColors.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        // Khởi tạo và gán vào biến toàn cục
        colorAdapter = ColorAdapter(colors) { colorStr ->
            activeTextItem?.textPaint?.shader = null
            activeTextItem?.setTextColor(Color.parseColor(colorStr))
            onStyleUpdated?.invoke()
        }
        binding.rvColors.adapter = colorAdapter

        // Font chữ
        val fonts = TextTemplateProvider.getFontList()
        binding.rvFonts.layoutManager = GridLayoutManager(requireContext(), 3)

        binding.rvFonts.setPadding(8, 8, 8, 8)
        binding.rvFonts.clipToPadding = false

        binding.rvFonts.adapter = FontAdapter(fonts) { fontPath ->
            try {
                val tf = Typeface.createFromAsset(requireContext().assets, "fonts/$fontPath")
                activeTextItem?.setTypeface(tf)
                onStyleUpdated?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // (Templates)
        val templates = TextTemplateProvider.getTemplates(requireContext().assets)
        binding.rvTemplates.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvTemplates.adapter = TemplateAdapter(templates) { template ->
            activeTextItem?.let { current ->
                current.applyStyleFrom(template)
                if (template.textPaint.shader == null) {
                    current.textPaint.shader = null
                }
                onStyleUpdated?.invoke()
            }
        }
    }
}