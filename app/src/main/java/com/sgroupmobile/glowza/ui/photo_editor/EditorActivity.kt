package com.sgroupmobile.glowza.ui.photo_editor

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextPaint
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayout
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseActivity
import com.sgroupmobile.glowza.common.enums.EditorTool
import com.sgroupmobile.glowza.common.enums.ToolType
import com.sgroupmobile.glowza.common.enums.constants.EXTRA_IMAGE_URI
import com.sgroupmobile.glowza.databinding.ActivityEditorBinding
import com.sgroupmobile.glowza.databinding.ItemTabToolBinding
import com.sgroupmobile.glowza.provider.EditorToolProvider
import com.sgroupmobile.glowza.ui.photo_editor.fragment.*
import com.sgroupmobile.glowza.util.FilterUtils
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class EditorActivity : BaseActivity<ActivityEditorBinding>() {

    private val viewModel: EditorViewModel by viewModels()

    override fun provideBinding(): ActivityEditorBinding = ActivityEditorBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMainTabs()
        handleIntentData()
    }

    override fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.baseBitmap.collectLatest { bitmap ->
                        bitmap?.let { binding.editorView.setBaseBitmap(it) }
                    }
                }

                launch {
                    viewModel.currentTool.collectLatest { type ->
                        type?.let { handleToolChange(it) }
                    }
                }
            }
        }
    }

    private fun handleIntentData() {
        intent.getStringExtra(EXTRA_IMAGE_URI)?.toUri()?.let {
            viewModel.loadImage(it)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnUndo.setOnClickListener {
            binding.editorView.undoLastDraw()
        }

        // Bạn có thể thêm btnSave vào đây để thực hiện xuất ảnh
        // binding.btnSave.setOnClickListener { saveImage() }
    }

    private fun setupMainTabs() {
        val tabLayout = binding.tabLayoutEditorTools
        val tools = EditorToolProvider.getPublicTools()

        tabLayout.removeAllTabs()

        tools.forEach { tool ->
            val tab = tabLayout.newTab().apply {
                val tabBinding = ItemTabToolBinding.inflate(layoutInflater)
                tabBinding.tabIcon.setImageResource(tool.icon)
                tabBinding.tabText.text = getString(tool.nameRes)
                customView = tabBinding.root
                tag = tool
            }
            tabLayout.addTab(tab)
            updateTabStyle(tab, false)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateTabStyle(tab, true)
                // Thông báo cho ViewModel biết công cụ nào được chọn
                (tab?.tag as? EditorTool)?.let { viewModel.selectTool(it.type) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) = updateTabStyle(tab, false)
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Ẩn hiện nhanh Sub-tool khi nhấn lại vào tab đang chọn
                binding.subToolContainer.visibility =
                    if (binding.subToolContainer.isVisible) View.GONE else View.VISIBLE
            }
        })
    }

    private fun handleToolChange(type: ToolType) {
        binding.editorView.setCropMode(false)
        binding.editorView.setDrawMode(false)
        binding.subToolContainer.visibility = View.GONE

        when (type) {
            ToolType.CROP -> showCropSubTool()
            ToolType.ADJUST -> showAdjustmentSubTool()
            ToolType.TEXT -> showTextSubTool()
            ToolType.DRAW -> showDrawSubTool()
            ToolType.FILTER -> openAssetSheet("Chọn Bộ Lọc", "filters")
            ToolType.STICKER -> openAssetSheet("Chọn Nhãn Dán", "stickers")
            ToolType.FRAME -> openAssetSheet("Chọn Khung Hình", "frames")
            else -> {}
        }
    }

    private fun replaceSubToolFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.sub_tool_container, fragment)
            .commit()
        binding.subToolContainer.visibility = View.VISIBLE
    }

    private fun showDrawSubTool() {
        val fragment = DrawSubToolFragment().apply {
            onDrawConfigChanged = { color, size, isEraser ->
                binding.editorView.setBrushConfig(color, size, isEraser)
            }
            onUndoClicked = { binding.editorView.undoLastDraw() }
            onClearClicked = { binding.editorView.clearAllDraw() }
        }
        binding.editorView.setDrawMode(true)
        replaceSubToolFragment(fragment)
    }
    private fun startUCrop(sourceUri: Uri) {
        // 1. Tạo file đích để lưu ảnh sau khi cắt (Lưu vào cache)
        val fileName = "Glowza_Crop_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, fileName))

        // 2. Cấu hình giao diện và tính năng cho uCrop
        val options = UCrop.Options().apply {
            setCompressionQuality(90) // Chất lượng ảnh 90%
            setToolbarColor(ContextCompat.getColor(this@EditorActivity, R.color.background))
            setStatusBarColor(ContextCompat.getColor(this@EditorActivity, R.color.background))

            // 1. Màu của tiêu đề Toolbar (Toolbar Title)

            // 2. Màu của nút "Done" (Dấu tích) và nút "Cancel" (Dấu X) trên Toolbar
            // Lưu ý: UCrop mặc định dùng chung màu với ToolbarWidgetColor cho 2 nút này
            setToolbarWidgetColor(ContextCompat.getColor(this@EditorActivity, R.color.primary))

            // 3. Màu sắc của các icon điều khiển bên dưới (Xoay, Tỉ lệ, ...) khi ĐƯỢC CHỌN
            setActiveControlsWidgetColor(ContextCompat.getColor(this@EditorActivity, R.color.primary))

            // 5. Màu của chữ hiển thị thông số (ví dụ: số độ khi xoay)
            setLogoColor(Color.TRANSPARENT) // Thường mình ẩn Logo UCrop cho chuyên nghiệp
            setHideBottomControls(false) // Hiện thanh điều khiển (Xoay, Tỉ lệ)
            setFreeStyleCropEnabled(true) // Cho phép kéo 4 góc tự do

            // Thiết lập các tỉ lệ cắt phổ biến
            setAspectRatioOptions(0,
                AspectRatio("Free", 0f, 0f),
                AspectRatio("1:1", 1f, 1f),
                AspectRatio("4:3", 4f, 3f),
                AspectRatio("3:2", 3f, 2f),
                AspectRatio("16:9", 16f, 9f)
            )
        }

        UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .start(this)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.let { uri ->
                viewModel.loadImage(uri)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
        }
    }

    private fun showCropSubTool() {
        val currentUri = intent.getStringExtra(EXTRA_IMAGE_URI)?.toUri()
        currentUri?.let { startUCrop(it) }
    }

    private fun showAdjustmentSubTool() {
        val fragment = AdjustmentSubToolFragment().apply {
            onAdjustmentChanged = { b, c, s -> binding.editorView.setAdjustments(b, c, s) }
        }
        replaceSubToolFragment(fragment)
    }

    private fun showTextSubTool() {
        val currentItem = binding.editorView.getSelectedItem()
        val targetItem = if (currentItem is TextItem) currentItem else {
            val newItem = TextItem("Glowza", TextPaint().apply {
                color = Color.WHITE
                textSize = 100f
                isAntiAlias = true
            })
            binding.editorView.addItem(newItem)
            newItem
        }

        val fragment = TextSubToolFragment().apply {
            setTargetItem(targetItem)
            onStyleUpdated = { binding.editorView.invalidate() }
            onTemplateSelected = { template ->
                targetItem.setTextColor(template.textPaint.color)
                targetItem.setTextSize(template.textPaint.textSize)
                targetItem.setTypeface(template.textPaint.typeface)
                targetItem.setTextAlpha(template.textPaint.alpha)
                binding.editorView.invalidate()
            }
        }
        replaceSubToolFragment(fragment)
    }

    private fun openAssetSheet(title: String, type: String) {
        val sheet = StickerBottomSheetFragment.newInstance(title, type) { resId ->
            when (type) {
                "stickers", "frames" -> {
                    val bitmap = BitmapFactory.decodeResource(resources, resId)
                    binding.editorView.addItem(StickerItem(bitmap))
                }
                "filters" -> applyFilterById(resId)
            }
        }
        sheet.show(supportFragmentManager, "AssetSheet")
    }

    private fun applyFilterById(id: Int) {
        val matrix = when (id) {
            101 -> FilterUtils.getOriginal()
            102 -> FilterUtils.getGrayScale()
            103 -> FilterUtils.getSepia()
            104 -> FilterUtils.getVintage()
            105 -> FilterUtils.getCold()
            else -> null
        }
        binding.editorView.setFilter(matrix)
    }

    private fun updateTabStyle(tab: TabLayout.Tab?, isSelected: Boolean) {
        val view = tab?.customView ?: return
        val icon = view.findViewById<ImageView>(R.id.tabIcon)
        val text = view.findViewById<TextView>(R.id.tabText)
        val color = if (isSelected) ContextCompat.getColor(this, R.color.primary) else Color.parseColor("#9E9E9E")
        icon.setColorFilter(color)
        text.setTextColor(color)
    }
}