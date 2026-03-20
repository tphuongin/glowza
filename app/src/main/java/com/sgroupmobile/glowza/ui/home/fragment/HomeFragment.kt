package com.sgroupmobile.glowza.ui.home.fragment

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.common.enums.GalleryMode
import com.sgroupmobile.glowza.data.model.FunctionItem
import com.sgroupmobile.glowza.data.model.FunctionType
import com.sgroupmobile.glowza.databinding.FragmentHomeBinding
import com.sgroupmobile.glowza.provider.FunctionProvider
import com.sgroupmobile.glowza.ui.camera.CameraActivity
import com.sgroupmobile.glowza.ui.collage.CollageActivity
import com.sgroupmobile.glowza.ui.gallery.GalleryActivity
import com.sgroupmobile.glowza.ui.home.FunctionAdapter
import com.sgroupmobile.glowza.ui.home.adapter.ImageAdapter
import com.sgroupmobile.glowza.ui.home.adapter.SliderAdapter

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    override fun provideBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentHomeBinding.inflate(inflater, container, false)

    override fun setupUI() {
        setupSlider()
        setupFunctionGrid()
        setupAds()
    }

    private fun setupSlider() {
        val banners = listOf(
            R.drawable.banner2,
            R.drawable.banner1
        )
        binding.viewPager.adapter = SliderAdapter(banners)
    }

    private fun setupFunctionGrid() {
        val functions = FunctionProvider.getFunctions()

        val layoutManager = GridLayoutManager(requireContext(), 4)

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (functions[position].isBig) 2 else 1
            }
        }

        binding.rvFunction.layoutManager = layoutManager
        binding.rvFunction.adapter = FunctionAdapter(functions) {
            onFunctionClicked(it)
        }
    }

    private fun onFunctionClicked(function: FunctionItem){
        when(function.type){
            FunctionType.EDIT -> {
                startActivity(
                    Intent(requireContext(), GalleryActivity::class.java).apply {
                        putExtra("mode", GalleryMode.SINGLE.name)
                    }
                )
            }

            FunctionType.CAMERA -> {
                startActivity(Intent(requireContext(), CameraActivity::class.java))
            }

            FunctionType.PHOTO_COLLAGE -> {
                startActivity(
                    Intent(requireContext(), GalleryActivity::class.java).apply {
                        putExtra("mode", GalleryMode.MULTIPLE.name)
                    }
                )
            }

            else -> {}
        }
    }

    private fun setupAds() {
        val ads = listOf(
            R.drawable.ic_draw,
            R.drawable.ic_sticker
        )

        binding.rvAds.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ImageAdapter(ads)
        }
    }
}