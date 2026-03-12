package com.sgroupmobile.glowza.ui.camera.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.databinding.FragmentCameraBinding

class CameraFragment : BaseFragment<FragmentCameraBinding>() {
    override fun provideBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)

    override fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnExit) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = systemBars.top
            v.layoutParams = params
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnSnap) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = systemBars.bottom
            v.layoutParams = params
            insets
        }
    }

}

