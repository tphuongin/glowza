package com.sgroupmobile.glowza.ui.camera

import android.Manifest
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseActivity
import com.sgroupmobile.glowza.databinding.ActivityCameraBinding
import com.sgroupmobile.glowza.helper.PermissionHelper
import com.sgroupmobile.glowza.ui.camera.fragment.CameraFragment

class CameraActivity : BaseActivity<ActivityCameraBinding>() {
    override fun provideBinding(): ActivityCameraBinding = ActivityCameraBinding.inflate(layoutInflater)
    private val permissionHelper = PermissionHelper(this)

    override fun setupUI() {
        checkPermission()
    }

    override fun setupInset() {}

    private fun checkPermission() {
        permissionHelper.requestPermission(
            Manifest.permission.CAMERA,
            R.layout.request_camera_permission
        ) {
            openCamera()
        }
    }

    private fun openCamera() {
        val currentFragment = supportFragmentManager.findFragmentById(binding.fragmentContainerView.id)
        if (currentFragment == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainerView.id, CameraFragment())
                .commit()
        }
    }
}