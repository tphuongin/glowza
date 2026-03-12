package com.sgroupmobile.glowza.ui.camera.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.databinding.FragmentImagePreviewBinding

class ImagePreviewFragment : BaseFragment<FragmentImagePreviewBinding>() {
    override fun provideBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentImagePreviewBinding = FragmentImagePreviewBinding.inflate(inflater, container, false)
}