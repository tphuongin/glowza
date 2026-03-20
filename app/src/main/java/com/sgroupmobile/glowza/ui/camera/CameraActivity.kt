package com.sgroupmobile.glowza.ui.camera

import android.Manifest
import android.os.Bundle
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseActivity
import com.sgroupmobile.glowza.databinding.ActivityCameraBinding
import com.sgroupmobile.glowza.helper.PermissionHelper
import com.sgroupmobile.glowza.ui.camera.fragment.CameraFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CameraActivity : BaseActivity<ActivityCameraBinding>() {
    override fun provideBinding(): ActivityCameraBinding = ActivityCameraBinding.inflate(layoutInflater)
    private lateinit var permissionHelper: PermissionHelper

    override fun setupUI() {
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        checkPermission()
    }

    override fun setupInset() {}
    private fun checkPermission() {
        permissionHelper = PermissionHelper(this, this)
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
                .addToBackStack(null)
                .commit()
        }
    }


}