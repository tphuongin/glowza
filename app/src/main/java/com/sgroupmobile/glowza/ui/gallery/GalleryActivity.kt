package com.sgroupmobile.glowza.ui.gallery

import android.Manifest
import android.content.ContentUris
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayout
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseActivity
import com.sgroupmobile.glowza.common.enum.GalleryTab
import com.sgroupmobile.glowza.data.model.GalleryImage
import com.sgroupmobile.glowza.databinding.ActivityGalleryBinding
import com.sgroupmobile.glowza.helper.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GalleryActivity : BaseActivity<ActivityGalleryBinding>() {
    private val viewModel: GalleryViewModel by viewModels()
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var permissionHelper: PermissionHelper

    override fun provideBinding(): ActivityGalleryBinding = ActivityGalleryBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupTabs()
        checkPermission()
    }

    private fun checkPermission() {
        permissionHelper = PermissionHelper(this)
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        permissionHelper.requestPermission(
            permission,
            R.layout.request_gallery_permission
        ) {
            // Sau khi có quyền, load ảnh lần đầu
            val images = fetchImagesFromDevice()

            viewModel.setAllPhotos(images)
        }
    }

    override fun initData() {
        galleryAdapter = GalleryAdapter { image ->
            // Logic: Mở màn hình xem chi tiết hoặc chỉnh sửa ảnh
            Log.d("Glowza", "Clicked: ${image.uri}")
        }
        binding.rvGallery.apply {
            adapter = galleryAdapter
            setHasFixedSize(true)
            itemAnimator = null // Tắt animation mặc định để tránh nháy khi đổi Tab
        }
    }

    private fun fetchImagesFromDevice(): List<GalleryImage> {
        val imageList = mutableListOf<GalleryImage>()
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
            val folderColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val favColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_FAVORITE)
            } else -1
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val folderName = cursor.getString(folderColumn) ?: "Camera"
                val date = cursor.getLong(dateColumn)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val isFav = if (favColumn != -1) cursor.getInt(favColumn) == 1 else false

                imageList.add(GalleryImage(id, uri, date, isFav, folderName))
            }
        }
        return imageList
    }

    override fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allPhotos.collect { list ->
                    // Chỉ cập nhật nếu Tab hiện tại là "Tất cả" (Position 0)
                    if (binding.tabLayoutFilters.selectedTabPosition == 0) {
                        galleryAdapter.submitList(list)
                    }
                }
            }
        }
    }

    private fun setupTabs() {
        // Giả sử GalleryTab có field 'tabName' (Tất cả, Yêu thích, Glowza)
        GalleryTab.entries.forEach {
            binding.tabLayoutFilters.addTab(binding.tabLayoutFilters.newTab().setText(it.name))
        }

        binding.tabLayoutFilters.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                lifecycleScope.launch {
                    when (tab?.position) {
                        0 -> viewModel.allPhotos.collect { galleryAdapter.submitList(it) }
                        1 -> viewModel.favoritePhotos.collect { galleryAdapter.submitList(it) }
                        2 -> viewModel.glowzaPhotos.collect { galleryAdapter.submitList(it) }
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
}