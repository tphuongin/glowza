package com.sgroupmobile.glowza.ui.photo_editor.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.sgroupmobile.glowza.base.BaseActivity
import com.sgroupmobile.glowza.databinding.ActivityFinishBinding
import com.sgroupmobile.glowza.ui.gallery.GalleryActivity
import com.sgroupmobile.glowza.ui.home.MainActivity

class FinishActivity : BaseActivity<ActivityFinishBinding>() {
    override fun provideBinding(): ActivityFinishBinding = ActivityFinishBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Nhận Uri từ EditorActivity
        val imageUriString = intent.getStringExtra("SAVED_IMAGE_URI")
        val imageUri = Uri.parse(imageUriString)
        binding.ivFinishedImage.setImageURI(imageUri)

        binding.btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        binding.btnBackToGallery.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        // Nút Chia sẻ hệ thống
        binding.btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
            }
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ ảnh qua..."))
        }
    }

}