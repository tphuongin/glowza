package com.sgroupmobile.glowza.ui.photo_editor.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayout
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseActivity
import com.sgroupmobile.glowza.common.enums.Constants
import com.sgroupmobile.glowza.common.enums.EditorTool
import com.sgroupmobile.glowza.common.enums.ToolType
import com.sgroupmobile.glowza.data.model.EditorAction
import com.sgroupmobile.glowza.data.model.StickerItem
import com.sgroupmobile.glowza.data.model.TextItem
import com.sgroupmobile.glowza.databinding.ActivityEditorBinding
import com.sgroupmobile.glowza.databinding.ItemTabToolBinding
import com.sgroupmobile.glowza.databinding.LayoutDialogCustomConfirmBinding
import com.sgroupmobile.glowza.databinding.LayoutDialogEditTextBinding
import com.sgroupmobile.glowza.extension.toBitmap
import com.sgroupmobile.glowza.provider.EditorToolProvider
import com.sgroupmobile.glowza.ui.photo_editor.EditorViewModel
import com.sgroupmobile.glowza.ui.photo_editor.fragment.AdjustmentSubToolFragment
import com.sgroupmobile.glowza.ui.photo_editor.fragment.DrawSubToolFragment
import com.sgroupmobile.glowza.ui.photo_editor.fragment.StickerBottomSheetFragment
import com.sgroupmobile.glowza.ui.photo_editor.fragment.TextSubToolFragment
import com.sgroupmobile.glowza.util.FilterUtils
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

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
        setupInset(binding.btnBack)
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
                launch {
                    viewModel.exportStatus.collect { uri ->
                        uri?.let {
                            Toast.makeText(this@EditorActivity, "Đã lưu vào bộ sưu tập!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@EditorActivity, FinishActivity::class.java).apply {
                                putExtra("SAVED_IMAGE_URI", it.toString())
                            }
                            startActivity(intent)
                            viewModel.resetExportStatus()
                        }
                    }
                }
            }
        }
    }
    private fun updateNavigationUI(state: EditorViewModel.NavigationState) {
        val activeColor = ContextCompat.getColor(this, R.color.onBackground)
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
        intent.getStringExtra(Constants.EXTRA_IMAGE_URI)?.toUri()?.let {
            viewModel.loadImage(it)
        }
    }


    fun confirmPendingAction() {
        when(pendingAction){
            is EditorAction.Frame ->{
                binding.editorView.setFramePreview(null)
            }
            is EditorAction.Filter -> {
                binding.editorView.resetPreviewFilters()
            }
            is EditorAction.Adjustment -> {
                binding.editorView.setAdjustments(0f,1f,1f)
            }
            else -> {}
        }
        pendingAction?.let {
            viewModel.addAction(it)
            pendingAction = null
        }

    }


    override fun setupListeners() {
        super.setupListeners()
        binding.btnSave.setOnClickListener {
            viewModel.saveImage()
        }

        binding.btnBack.setOnClickListener {

            if (binding.subToolContainer.isVisible) {
                cancelPendingAction()
                closeSubTool()
            } else {
                confirmExit()
            }
        }
        binding.editorView.onTextItemDoubleClicked = { textItem ->
            showEditTextDialog(textItem)
        }

        binding.btnUndo.setOnClickListener { viewModel.undo() }
        binding.btnRedo.setOnClickListener { viewModel.redo() }
    }
    private fun confirmExit() {
        val dialogBinding = LayoutDialogCustomConfirmBinding.inflate(layoutInflater)

        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        // lấy thông số kích thước màn hình điện thoại
        val displayMetrics = resources.displayMetrics
        val dialogWidth = (displayMetrics.widthPixels * 0.85).toInt()
        dialog.window?.setLayout(
            dialogWidth,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.btnStay.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDiscard.setOnClickListener {
            dialog.dismiss()
            finish()
        }
        dialog.show()
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

        val sheet = StickerBottomSheetFragment.Companion.newInstance(title, type) { id ->
            handlePreviewChange(type, id)
        }
        sheet.show(supportFragmentManager, "AssetSheet")
        // Giữ nguyên delay clear style
        binding.tabLayoutEditorTools.postDelayed({ clearAllTabStyles() }, 100)
    }

    private fun handlePreviewChange(type: String, id: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, id)
        when (type) {
            "filters" -> {
                val matrix = FilterUtils.getMatrixById(id)
                matrix?.let {
                    binding.editorView.setFilter(it)
                    pendingAction = EditorAction.Filter(it)
                }
            }
            "stickers" -> {
                val bitmap = BitmapFactory.decodeResource(resources, id)
                val item = StickerItem(bitmap).apply { }
                viewModel.addNewItem(item)

                viewModel.addAction(
                    EditorAction.Sticker(
                    bitmap,
                    item.matrix,
                    item.id,
                        Matrix(binding.editorView.getBaseMatrix())
                ))
            }
            "frames" -> {
                binding.editorView.setFramePreview(bitmap)
                pendingAction = EditorAction.Frame(bitmap)
            }
        }
    }

    private fun showCropSubTool() {
        val uriToCrop = viewModel.currentUri.value ?: intent.getStringExtra(Constants.EXTRA_IMAGE_URI)?.toUri()
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

    private fun replaceSubToolFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.sub_tool_container, fragment)
            .commitNowAllowingStateLoss()

        binding.subToolContainer.visibility = View.VISIBLE
    }
    private fun showAdjustmentSubTool() = replaceSubToolFragment(AdjustmentSubToolFragment().apply {
        onAdjustmentChanged = { b, c, s ->
            binding.editorView.setAdjustments(b, c, s)
            pendingAction = EditorAction.Adjustment(b, c, s)
        }
    })

    fun cancelPendingAction() {
        val action = pendingAction ?: return
        when (action) {
            is EditorAction.Frame -> binding.editorView.setFramePreview(null) // Xóa preview frame
            is EditorAction.Filter -> binding.editorView.resetPreviewFilters()
            is EditorAction.Adjustment -> binding.editorView.setAdjustments(0f, 1f, 1f)
            is EditorAction.Sticker, is EditorAction.Text -> binding.editorView.removeLastItemIfPending()
            else -> viewModel.renderImage()
        }
        pendingAction = null
    }

    fun closeSubTool() {
        pendingAction = null // Xóa bỏ mọi pending đang treo
        viewModel.resetTool() // Bắn tin hiệu để ẩn sub_tool_container
    }

    private fun handleToolChange(type: ToolType) {

        binding.editorView.setDrawMode(type == ToolType.DRAW)
        when (type) {
            ToolType.ADJUST -> showAdjustmentSubTool()
            ToolType.TEXT -> {
                showTextSubTool()
                val lastText = viewModel.itemList.value.filterIsInstance<TextItem>().lastOrNull()
                lastText?.let {
                    it.isSelected = true
                    binding.editorView.setSelectedItem(it)
                }
            }
            ToolType.DRAW -> showDrawSubTool()
            else -> {
                binding.subToolContainer.visibility = View.GONE
            }
        }
    }
    fun confirmDrawAction() {
        val currentPaths = binding.editorView.getDrawPaths()
        if (currentPaths.isNotEmpty()) {
            val currentDisplayMatrix = binding.editorView.getBaseMatrix()

            val drawAction = EditorAction.Draw(
                ArrayList(currentPaths),
                Matrix(currentDisplayMatrix) // Phải tạo bản sao Matrix mới
            )
            viewModel.addAction(drawAction)
            binding.editorView.clearAllDraw()
        }
        closeSubTool()
    }
    private fun showDrawSubTool() = replaceSubToolFragment(DrawSubToolFragment().apply {
        onDrawConfigChanged = { color, size, isEraser -> binding.editorView.setBrushConfig(color, size, isEraser) }
        onUndoClicked = { binding.editorView.undoLastDraw() }
        onClearClicked = { binding.editorView.clearAllDraw() }
        onRedoClicked = { binding.editorView.redoLastDraw() }
    })
    private fun showEditTextDialog(textItem: TextItem) {
        val dialog = Dialog(this)
        val bindingDialog = LayoutDialogEditTextBinding.inflate(layoutInflater)
        dialog.setContentView(bindingDialog.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        bindingDialog.edtInput.setText(textItem.text)
        bindingDialog.edtInput.selectAll()
        bindingDialog.edtInput.requestFocus()

        bindingDialog.btnCancel.setOnClickListener { dialog.dismiss() }

        bindingDialog.btnConfirm.setOnClickListener {
            val newText = bindingDialog.edtInput.text.toString()
            if (newText.isNotEmpty()) {
                textItem.updateText(newText)
                binding.editorView.invalidate()
                dialog.dismiss()
            }
        }

        dialog.show()
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = (resources.displayMetrics.widthPixels * 0.9).toInt() // Chiếm 90% màn hình
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = layoutParams

        // Tự động hiện bàn phím
        bindingDialog.edtInput.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(bindingDialog.edtInput, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }
    private fun showTextSubTool() {
        val currentSelectedItem = binding.editorView.getSelectedItem()

        // Lấy ma trận hiển thị hiện tại để làm mốc tọa độ
        val currentDisplayMatrix = Matrix(binding.editorView.getBaseMatrix())

        val targetItem = if (currentSelectedItem is TextItem) {
            currentSelectedItem
        } else {
            val newItem = TextItem("Glowza").apply {
                id = System.currentTimeMillis()
                textPaint.apply {
                    color = Color.WHITE
                    textSize = 100f
                    isAntiAlias = true
                }
            }
            viewModel.addNewItem(newItem)

            // Thêm currentDisplayMatrix vào đây để Engine không bị lỗi
            viewModel.addAction(
                EditorAction.Text(
                newItem.text,
                newItem.matrix,
                newItem.textPaint,
                newItem.id,
                currentDisplayMatrix
            ))
            newItem
        }

        replaceSubToolFragment(TextSubToolFragment().apply {
            setTargetItem(targetItem)

            onStyleUpdated = {
                binding.editorView.invalidate()

                val updatedDisplayMatrix = Matrix(binding.editorView.getBaseMatrix())
                viewModel.updateTextAction(targetItem, updatedDisplayMatrix)
            }

            onTemplateSelected = { template ->
                val activeItem = binding.editorView.getSelectedItem()
                val templateDisplayMatrix = Matrix(binding.editorView.getBaseMatrix())

                if (activeItem is TextItem) {
                    activeItem.applyStyleFrom(template)
                    viewModel.updateTextAction(activeItem, templateDisplayMatrix)
                    binding.editorView.invalidate()
                } else {
                    val createdItem = TextItem("Glowza").apply {
                        id = System.currentTimeMillis()
                        applyStyleFrom(template)
                    }

                    viewModel.addNewItem(createdItem)

                    viewModel.addAction(
                        EditorAction.Text(
                        text = createdItem.text,
                        matrix = createdItem.matrix,
                        textPaint = createdItem.textPaint,
                        id = createdItem.id,
                        displayMatrix = templateDisplayMatrix
                    ))

                    setTargetItem(createdItem)
                    binding.editorView.invalidate()
                }
            }
        })
    }
    private fun updateTabStyle(tab: TabLayout.Tab?, isSelected: Boolean) {
        val view = tab?.customView ?: return
        val icon = view.findViewById<ImageView>(R.id.tabIcon)
        val text = view.findViewById<TextView>(R.id.tabText)
        val color = if (isSelected) ContextCompat.getColor(this, R.color.primary) else ContextCompat.getColor(this, R.color.onBackground)
        icon.setColorFilter(color)
        text.setTextColor(color)
    }

}