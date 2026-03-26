package com.sgroupmobile.glowza.ui.photo_editor.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseActivity
import com.sgroupmobile.glowza.databinding.ActivityFinishBinding
import com.sgroupmobile.glowza.ui.gallery.GalleryActivity
import com.sgroupmobile.glowza.ui.home.MainActivity
import com.sgroupmobile.glowza.ui.home.adapter.ImageAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FinishActivity : BaseActivity<ActivityFinishBinding>() {

    private var savedImageUri: Uri? = null

    override fun provideBinding(): ActivityFinishBinding = ActivityFinishBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lấy Uri ảnh được truyền sang từ EditorActivity
        val imageUriString = intent.getStringExtra("SAVED_IMAGE_URI")
        if (imageUriString != null) {
            savedImageUri = Uri.parse(imageUriString)
            binding.ivFinishedImage.setImageURI(savedImageUri)
        }
        setupAds()
    }

    override fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnHome.setOnClickListener {
            navigateToHome()
        }

        binding.btnEditMore.setOnClickListener {
            finish()
        }

        binding.btnBackToGallery.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        binding.btnShareMessenger.setOnClickListener {
            shareImageToApp("com.facebook.orca")
        }

        binding.btnShareInstagram.setOnClickListener {
            shareImageToApp("com.instagram.android")
        }

        binding.btnShareFacebook.setOnClickListener {
            shareImageToApp("com.facebook.katana")
        }

        binding.btnShareWhatsapp.setOnClickListener {
            shareImageToApp("com.whatsapp")
        }

        binding.btnShareMore.setOnClickListener {
            shareImageToApp("com.zhiliaoapp.musically")
        }
    }
    private fun shareImageToApp(packageName: String?) {
        val uri = savedImageUri ?: return

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (packageName != null) {
                setPackage(packageName)
            }
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Nếu máy không cài app đó, sẽ văng lỗi -> Catch lại và mở bảng Share mặc định
            Toast.makeText(this, "Ứng dụng chưa được cài đặt", Toast.LENGTH_SHORT).show()

            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(fallbackIntent, getString(R.string.share_to)))
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun setupAds() {
        val ads = listOf(
            R.drawable.ad10, R.drawable.ad12,
            R.drawable.ad11, R.drawable.ad13
        )

        binding.rvAds.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = ImageAdapter(ads)
        }
    }
}