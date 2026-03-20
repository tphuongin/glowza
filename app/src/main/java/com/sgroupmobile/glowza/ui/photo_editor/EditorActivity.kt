package com.sgroupmobile.glowza.ui.photo_editor

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextPaint
import android.util.Log
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
import com.sgroupmobile.glowza.common.enums.Constants.EXTRA_IMAGE_URI
import com.sgroupmobile.glowza.data.model.EditorAction
import com.sgroupmobile.glowza.databinding.ActivityEditorBinding
import com.sgroupmobile.glowza.databinding.ItemTabToolBinding
import com.sgroupmobile.glowza.extension.toBitmap
import com.sgroupmobile.glowza.provider.EditorToolProvider
import com.sgroupmobile.glowza.ui.photo_editor.fragment.*
import com.sgroupmobile.glowza.util.FilterUtils
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import com.sgroupmobile.glowza.data.model.StickerItem
import com.sgroupmobile.glowza.data.model.TextItem
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewModelScope

@AndroidEntryPoint
class EditorActivity : BaseActivity<ActivityEditorBinding>() {

    val viewModel: EditorViewModel by viewModels()


    private var pendingAction: EditorAction? = null

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
                launch {
                    viewModel.previewBitmap.collectLatest { bitmap ->
                        bitmap?.let { binding.editorView.setBaseBitmap(it) }
                    }
                }
                launch {
                    viewModel.itemList.collectLatest { list ->
                        binding.editorView.updateItems(list)
                        binding.editorView.invalidate()
                    }
                }

                launch {
                    viewModel.navigationState.collect { state ->
                        updateNavigationUI(state)
                    }
                }

                launch {
                    viewModel.currentTool.collectLatest { type ->
                        if (type != null) {
                            handleToolChange(type)
                        } else {
                            binding.subToolContainer.visibility = View.GONE
                            binding.editorView.setDrawMode(false)
                            clearAllTabStyles()
                        }
                    }
                }
            }
        }
    }

    private fun updateNavigationUI(state: EditorViewModel.NavigationState) {
        val activeColor = ContextCompat.getColor(this, R.color.primary)
        val inactiveColor = "#9E9E9E".toColorInt()

        binding.btnUndo.apply {
            isEnabled = state.canUndo
            setColorFilter(if (state.canUndo) activeColor else inactiveColor)
            alpha = if (state.canUndo) 1.0f else 0.4f
        }

        binding.btnRedo.apply {
            isEnabled = state.canRedo
            setColorFilter(if (state.canRedo) activeColor else inactiveColor)
            alpha = if (state.canRedo) 1.0f else 0.4f
        }
    }

    private fun handleIntentData() {
        intent.getStringExtra(EXTRA_IMAGE_URI)?.toUri()?.let {
            viewModel.loadImage(it)
        }
    }


    fun confirmPendingAction() {
        pendingAction?.let {
            viewModel.addAction(it)
            pendingAction = null
        }
    }

    override fun setupListeners() {
        super.setupListeners()

        binding.btnBack.setOnClickListener {

            if (binding.subToolContainer.isVisible) {
                // Nếu thanh công cụ đang hiện, cứ đóng nó lại đã
                cancelPendingAction()
                closeSubTool()
            } else {
                finish()
            }
        }

        binding.btnUndo.setOnClickListener { viewModel.undo() }
        binding.btnRedo.setOnClickListener { viewModel.redo() }
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
                val tool = tab?.tag as? EditorTool ?: return

                when (tool.type) {
                    ToolType.CROP -> showCropSubTool()
                    ToolType.FILTER -> openAssetSheet("Chọn Bộ Lọc", "filters")
                    ToolType.STICKER -> openAssetSheet("Chọn Nhãn Dán", "stickers")
                    ToolType.FRAME -> openAssetSheet("Chọn Khung Hình", "frames")
                    else -> viewModel.selectTool(tool.type)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = updateTabStyle(tab, false)

            override fun onTabReselected(tab: TabLayout.Tab?) {
                val tool = tab?.tag as? EditorTool ?: return
                if (tool.type == ToolType.CROP || tool.type == ToolType.FILTER ||
                    tool.type == ToolType.STICKER || tool.type == ToolType.FRAME) {
                    onTabSelected(tab)
                } else {
                    if (binding.subToolContainer.isVisible) {
                        viewModel.resetTool()
                    } else {
                        binding.subToolContainer.visibility = View.VISIBLE
                        updateTabStyle(tab, true)
                    }
                }
            }
        })
    }

    fun getPendingAction(): EditorAction? = pendingAction


    private fun clearAllTabStyles() {
        val tabLayout = binding.tabLayoutEditorTools
        for (i in 0 until tabLayout.tabCount) {
            updateTabStyle(tabLayout.getTabAt(i), false)
        }
    }

    private fun openAssetSheet(title: String, type: String) {
        if (supportFragmentManager.findFragmentByTag("AssetSheet") != null) return

        if (type == "filters") {
            viewModel.prepareFilterPreviews()
        }

        val sheet = StickerBottomSheetFragment.newInstance(title, type) { id ->
            // Chỉ truyền ID vào, việc xử lý nằm ở Activity
            handlePreviewChange(type, id)
        }
        sheet.show(supportFragmentManager, "AssetSheet")
        // Giữ nguyên delay clear style
        binding.tabLayoutEditorTools.postDelayed({ clearAllTabStyles() }, 100)
    }

    private fun handlePreviewChange(type: String, id: Int) {
        when (type) {
            "filters" -> {
                val matrix = FilterUtils.getMatrixById(id)
                matrix?.let {
                    binding.editorView.setFilter(it)
                    pendingAction = EditorAction.Filter(it)
                    // Đối với Filter, nhấn là chốt ngay
                }
            }
            "stickers", "frames" -> {
                // SỬA TẠI ĐÂY: Chỉ thêm vào EditorView để người dùng di chuyển/xoay/phóng to
                // KHÔNG gán vào pendingAction và KHÔNG confirm ngay lập tức
                val bitmap = BitmapFactory.decodeResource(resources, id)
                val item = StickerItem(bitmap)
//                binding.editorView.addItem(item)
                viewModel.addNewItem(item)
                viewModel.addAction(EditorAction.Sticker(bitmap, item.matrix))

                // Sticker sẽ được xác nhận vào ViewModel khi người dùng nhấn nút "Save"
                // hoặc khi chuyển Tool (tùy logic bạn muốn)
            }
        }
    }
    // --- CÁC HÀM CŨ GIỮ NGUYÊN ---

    private fun showCropSubTool() {
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
            setFreeStyleCropEnabled(true)
        }
        UCrop.of(sourceUri, destinationUri).withOptions(options).start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.resetTool()
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.toBitmap(this)?.let { bitmap ->
                viewModel.addAction(EditorAction.Crop(bitmap))
            }
        }
        clearAllTabStyles()
    }

    private fun replaceSubToolFragment(fragment: androidx.fragment.app.Fragment) {
        // Thêm Log để kiểm tra
        Log.d("Glowza_Logic", "Activity: replaceSubToolFragment gọi")

        supportFragmentManager.beginTransaction()
            .replace(R.id.sub_tool_container, fragment)
            // Đảm bảo commitNow để Fragment mới được đưa vào ngay lập tức
            .commitNowAllowingStateLoss()

        binding.subToolContainer.visibility = View.VISIBLE
    }
    private fun showAdjustmentSubTool() = replaceSubToolFragment(AdjustmentSubToolFragment().apply {
        onAdjustmentChanged = { b, c, s ->
            // 1. Hiển thị preview lên View ngay lập tức
            binding.editorView.setAdjustments(b, c, s)

            // 2. Lưu vào pendingAction để chờ xác nhận hoặc hủy
            // Bạn cần đảm bảo EditorAction có thêm class Adjust(b, c, s)
            pendingAction = EditorAction.Adjustment(b, c, s)
        }
    })

    // Cập nhật hàm cancelPendingAction để xử lý thêm Adjust
    // 1. Hàm hủy Preview (Chỉ lo phần ảnh)
    fun cancelPendingAction() {
        val action = pendingAction ?: return
        when (action) {
            is EditorAction.Filter -> binding.editorView.resetPreviewFilters()
            is EditorAction.Adjustment -> binding.editorView.setAdjustments(0f, 1f, 1f)
            is EditorAction.Sticker, is EditorAction.Text -> binding.editorView.removeLastItemIfPending()
            else -> viewModel.renderImage()
        }
        pendingAction = null
    }

    // 2. Hàm đóng giao diện (Chỉ lo phần UI)
    fun closeSubTool() {
        Log.d("Glowza_Logic", "Activity: closeSubTool gọi. Đang resetTool trong ViewModel")
        pendingAction = null // Xóa bỏ mọi pending đang treo
        viewModel.resetTool() // Bắn tin hiệu để ẩn sub_tool_container
    }

    // 3. Sửa lại handleToolChange để tránh vòng lặp
    private fun handleToolChange(type: ToolType) {
        Log.d("Glowza_Logic", "Activity: handleToolChange sang tool $type")
        // KHÔNG gọi cancelPendingAction ở đây nữa,
        // để Fragment tự lo việc dọn dẹp khi nó bị thay thế.

        binding.editorView.setDrawMode(type == ToolType.DRAW)
        when (type) {
            ToolType.ADJUST -> showAdjustmentSubTool()
            ToolType.TEXT -> showTextSubTool()
            ToolType.DRAW -> showDrawSubTool()
            else -> {
                binding.subToolContainer.visibility = View.GONE
            }
        }
    }
    private fun showDrawSubTool() = replaceSubToolFragment(DrawSubToolFragment().apply {
        onDrawConfigChanged = { color, size, isEraser -> binding.editorView.setBrushConfig(color, size, isEraser) }
        onUndoClicked = { binding.editorView.undoLastDraw() }
        onClearClicked = { binding.editorView.clearAllDraw() }
    })

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
        replaceSubToolFragment(TextSubToolFragment().apply {
            setTargetItem(targetItem)
            onStyleUpdated = { binding.editorView.invalidate() }
        })
    }

    private fun updateTabStyle(tab: TabLayout.Tab?, isSelected: Boolean) {
        val view = tab?.customView ?: return
        val icon = view.findViewById<ImageView>(R.id.tabIcon)
        val text = view.findViewById<TextView>(R.id.tabText)
        val color = if (isSelected) ContextCompat.getColor(this, R.color.primary) else "#9E9E9E".toColorInt()
        icon.setColorFilter(color)
        text.setTextColor(color)
    }

}