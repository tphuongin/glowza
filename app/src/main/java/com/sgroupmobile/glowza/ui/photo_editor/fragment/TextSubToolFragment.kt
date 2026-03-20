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
import com.sgroupmobile.glowza.data.model.TextItem
import com.sgroupmobile.glowza.provider.ColorProvider
import com.sgroupmobile.glowza.provider.TextTemplateProvider
import com.sgroupmobile.glowza.ui.photo_editor.adapter.ColorAdapter
import com.sgroupmobile.glowza.ui.photo_editor.adapter.FontAdapter
import com.sgroupmobile.glowza.ui.photo_editor.adapter.TemplateAdapter

class TextSubToolFragment : Fragment() {
    private var _binding: LayoutSubToolTextBinding? = null
    private val binding get() = _binding!!

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

    fun setTargetItem(item: TextItem) {
        this.activeTextItem = item
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
        val colors = ColorProvider.getEditorColors()
        binding.rvColors.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvColors.adapter = ColorAdapter(colors) { colorStr ->
            activeTextItem?.setTextColor(Color.parseColor(colorStr))
            onStyleUpdated?.invoke()
        }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}