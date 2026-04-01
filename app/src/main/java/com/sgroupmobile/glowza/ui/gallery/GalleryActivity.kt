package com.sgroupmobile.glowza.ui.gallery

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayout
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseActivity
import com.sgroupmobile.glowza.common.enums.Constants.EXTRA_IMAGE_URI
import com.sgroupmobile.glowza.common.enums.GalleryMode
import com.sgroupmobile.glowza.common.enums.GalleryTab
import com.sgroupmobile.glowza.data.model.GalleryItem
import com.sgroupmobile.glowza.databinding.ActivityGalleryBinding
import com.sgroupmobile.glowza.helper.PermissionHelper
import com.sgroupmobile.glowza.ui.photo_editor.activity.EditorActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GalleryActivity : BaseActivity<ActivityGalleryBinding>() {
    private val viewModel: GalleryViewModel by viewModels()
    private lateinit var permissionHelper: PermissionHelper

    private val mode: GalleryMode by lazy {
        GalleryMode.valueOf(
            intent.getStringExtra("mode") ?: GalleryMode.SINGLE.name
        )
    }

    override fun provideBinding(): ActivityGalleryBinding =
        ActivityGalleryBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupTabs()
        checkPermission()
        setupInset(binding.toolbar)
        handleIntentTab()
    }
    private fun handleIntentTab() {
        val targetTabName = intent.getStringExtra("target_tab")

        if (targetTabName != null) {
            val targetTab = GalleryTab.valueOf(targetTabName)

            val position = when (targetTab) {
                GalleryTab.ALL -> 0
                GalleryTab.FAVOURITE -> 1
                GalleryTab.GLOWZA -> 2
                GalleryTab.VIDEO -> 3
            }
            binding.tabLayoutFilters.getTabAt(position)?.select()
        }
    }
    private fun checkPermission() {
        permissionHelper = PermissionHelper(this, this)
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        permissionHelper.requestPermission(
            permission,
            R.layout.request_gallery_permission
        ) {
            val images = fetchImagesFromDevice()
            val videos = fetchAppVideosFromDevice()
            viewModel.setImages(images)
            viewModel.setVideos(videos)
        }
    }

    private fun fetchImagesFromDevice(): List<GalleryItem> {
        val imageList = mutableListOf<GalleryItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.IS_FAVORITE else MediaStore.Images.Media._ID
        )

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val folderColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val favColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_FAVORITE)
            } else -1
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val folderName = cursor.getString(folderColumn) ?: "Camera"
                val date = cursor.getLong(dateColumn)
                val uri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val isFav = if (favColumn != -1) cursor.getInt(favColumn) == 1 else false

                imageList.add(GalleryItem(id, uri, date, isFav, folderName))
            }
        }
        return imageList
    }

    private fun fetchAppVideosFromDevice(): List<GalleryItem> {
        val videoList = mutableListOf<GalleryItem>()

        // Khai báo các cột cần lấy dữ liệu
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.IS_FAVORITE else MediaStore.Video.Media._ID
        )

        // Lọc theo DISPLAY_NAME bắt đầu bằng "Glowza_Video_"
        val selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("Glowza_Video_%")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val folderColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val favColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.IS_FAVORITE)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val folderName = cursor.getString(folderColumn) ?: "Glowza"
                val date = cursor.getLong(dateColumn)

                // Tạo URI cho Video
                val uri =
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                // Tránh lỗi khi SDK < Q (cột IS_FAVORITE gán tạm bằng _ID)
                val isFav =
                    if (favColumn != -1 && favColumn != idColumn) cursor.getInt(favColumn) == 1 else false

                videoList.add(GalleryItem(id, uri, date, isFav, folderName,
                    isSelected = false,
                    isVideo = true
                ))
            }
        }
        return videoList
    }

    override fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.images.collect { list ->
                    filterDataByTab(binding.tabLayoutFilters.selectedTabPosition)
                }
            }
        }
    }

    override fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupTabs() {
        GalleryTab.entries.forEach {
            binding.tabLayoutFilters.addTab(binding.tabLayoutFilters.newTab().setText(it.name))
        }

        binding.tabLayoutFilters.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterDataByTab(tab?.position ?: 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(p0: TabLayout.Tab?) {}
        })
    }

    private fun filterDataByTab(position: Int) {
        val tabCategory = when (position) {
            0 -> GalleryTab.ALL
            1 -> GalleryTab.FAVOURITE
            2 -> GalleryTab.GLOWZA
            3 -> GalleryTab.VIDEO
            else -> GalleryTab.ALL
        }

        val fragment = GalleryFragment.newInstance(tabCategory)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}