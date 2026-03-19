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
        setupObservers()
        setupMainTabs()
        setupListeners()
        handleIntentData()
    }

    override fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Quan sát Bitmap gốc để hiển thị lên EditorView
                launch {
                    viewModel.baseBitmap.collectLatest { bitmap ->
                        bitmap?.let { binding.editorView.setBaseBitmap(it) }
                    }
                }

                // Quan sát Tool hiện tại để hiển thị SubTool tương ứng
                launch {
                    viewModel.currentTool.collectLatest { type ->
                        // Lưu ý: ToolType.CROP không xử lý ở đây để tránh loop khi quay về từ UCrop
                        if (type != null && type != ToolType.CROP) {
                            handleToolChange(type)
                        } else if (type == null) {
                            // Nếu tool là null, ẩn subtool container
                            binding.subToolContainer.visibility = View.GONE
                            binding.editorView.setDrawMode(false)
                        }
                    }
                }
            }
        }
    }

    private fun handleIntentData() {
        // Lấy URI từ Intent và tải ảnh vào ViewModel
        intent.getStringExtra(EXTRA_IMAGE_URI)?.toUri()?.let {
            viewModel.loadImage(it)
        }
    }

    override fun setupListeners() {
        super.setupListeners()
        binding.btnBack.setOnClickListener { finish() }
        binding.btnUndo.setOnClickListener { binding.editorView.undoLastDraw() }
    }

    private fun setupMainTabs() {
        val tabLayout = binding.tabLayoutEditorTools
        val tools = EditorToolProvider.getPublicTools()

        tabLayout.removeAllTabs()

        // Tạo các Tab dựa trên danh sách tool
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
                val tool = tab?.tag as? EditorTool ?: return

                if (tool.type == ToolType.CROP) {
                    // Mở màn hình Crop ngay lập tức
                    showCropSubTool()
                } else {
                    // Chuyển tool trong ViewModel cho các loại khác
                    viewModel.selectTool(tool.type)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = updateTabStyle(tab, false)

            override fun onTabReselected(tab: TabLayout.Tab?) {
                val tool = tab?.tag as? EditorTool ?: return
                if (tool.type == ToolType.CROP) {
                    // Khắc phục việc nhấn lại Tab Crop khi đang chọn mà không phản hồi
                    showCropSubTool()
                } else {
                    // Đóng/Mở SubTool khi nhấn lại tab hiện tại
                    binding.subToolContainer.visibility =
                        if (binding.subToolContainer.isVisible) View.GONE else View.VISIBLE
                }
            }
        })
    }

    private fun handleToolChange(type: ToolType) {
        // Reset trạng thái vẽ trước khi chuyển tool
        binding.editorView.setDrawMode(false)
        binding.subToolContainer.visibility = View.GONE

        when (type) {
            ToolType.ADJUST -> showAdjustmentSubTool()
            ToolType.TEXT -> showTextSubTool()
            ToolType.DRAW -> showDrawSubTool()
            ToolType.FILTER -> openAssetSheet("Chọn Bộ Lọc", "filters")
            ToolType.STICKER -> openAssetSheet("Chọn Nhãn Dán", "stickers")
            ToolType.FRAME -> openAssetSheet("Chọn Khung Hình", "frames")
            else -> {}
        }
    }

    /**
     * Khởi chạy UCrop với URI ảnh mới nhất (ảnh đã chỉnh sửa)
     */
    private fun showCropSubTool() {
        // Quan trọng: Lấy URI hiện tại từ ViewModel (ảnh đã qua xử lý trước đó)
        // Nếu ViewModel chưa có currentUri, fallback về Intent ban đầu
        val uriToCrop = viewModel.currentUri.value ?: intent.getStringExtra(EXTRA_IMAGE_URI)?.toUri()

        uriToCrop?.let { startUCrop(it) }
    }

    private fun startUCrop(sourceUri: Uri) {
        val fileName = "Glowza_Edited_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, fileName))

        val options = UCrop.Options().apply {
            setCompressionQuality(90)
            setToolbarColor(ContextCompat.getColor(this@EditorActivity, R.color.background))
            setStatusBarColor(ContextCompat.getColor(this@EditorActivity, R.color.background))
            setToolbarWidgetColor(ContextCompat.getColor(this@EditorActivity, R.color.primary))
            setActiveControlsWidgetColor(ContextCompat.getColor(this@EditorActivity, R.color.primary))
            setLogoColor(Color.TRANSPARENT)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)

            // Cấu hình các tỉ lệ cắt mặc định
            setAspectRatioOptions(0,
                AspectRatio("Tự do", 0f, 0f),
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

        // Reset trạng thái Tool về null ngay lập tức để tránh loop khi quay lại
        viewModel.resetTool()

        if (requestCode == UCrop.REQUEST_CROP) {
            if (resultCode == RESULT_OK && data != null) {
                val resultUri = UCrop.getOutput(data)
                resultUri?.let { uri ->
                    // Tải ảnh đã crop mới nhất vào Editor
                    viewModel.loadImage(uri)
                }
            } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
                val cropError = UCrop.getError(data)
                // Xử lý thông báo lỗi nếu cần
            }
        }
    }

    private fun replaceSubToolFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.sub_tool_container, fragment)
            .commit()
        binding.subToolContainer.visibility = View.VISIBLE
    }

    private fun showAdjustmentSubTool() {
        val fragment = AdjustmentSubToolFragment().apply {
            onAdjustmentChanged = { b, c, s -> binding.editorView.setAdjustments(b, c, s) }
        }
        replaceSubToolFragment(fragment)
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
                "filters" -> {
                    // Logic áp dụng bộ lọc theo ID
                    applyFilterById(resId)
                }
            }
        }
        sheet.show(supportFragmentManager, "AssetSheet")
    }

    private fun applyFilterById(id: Int) {
        val matrix = when (id) {
            101 -> FilterUtils.getOriginal()
            102 -> FilterUtils.getGrayScale()
            103 -> FilterUtils.getSepia()
            else -> null
        }
        binding.editorView.setFilter(matrix)
    }

    private fun updateTabStyle(tab: TabLayout.Tab?, isSelected: Boolean) {
        val view = tab?.customView ?: return
        val icon = view.findViewById<ImageView>(R.id.tabIcon)
        val text = view.findViewById<TextView>(R.id.tabText)
        val color = if (isSelected) {
            ContextCompat.getColor(this, R.color.primary)
        } else {
            Color.parseColor("#9E9E9E")
        }
        icon.setColorFilter(color)
        text.setTextColor(color)
    }
}