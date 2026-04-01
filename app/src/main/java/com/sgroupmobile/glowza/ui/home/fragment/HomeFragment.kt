package com.sgroupmobile.glowza.ui.home.fragment

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.common.enums.GalleryMode
import com.sgroupmobile.glowza.common.enums.GalleryTab
import com.sgroupmobile.glowza.data.model.FunctionItem
import com.sgroupmobile.glowza.data.model.FunctionType
import com.sgroupmobile.glowza.databinding.FragmentHomeBinding
import com.sgroupmobile.glowza.provider.FunctionProvider
import com.sgroupmobile.glowza.ui.camera.CameraActivity
import com.sgroupmobile.glowza.ui.gallery.GalleryActivity
import com.sgroupmobile.glowza.ui.home.adapter.FunctionAdapter
import com.sgroupmobile.glowza.ui.home.MainActivity
import com.sgroupmobile.glowza.ui.home.adapter.ImageAdapter
import com.sgroupmobile.glowza.ui.home.adapter.SliderAdapter
import kotlin.math.abs

class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private val sliderHandler = Handler(Looper.getMainLooper())
    private val sliderRunnable = Runnable {
        val current = binding.viewPager.currentItem
        val total = binding.viewPager.adapter?.itemCount ?: 0
        if (total > 0) {
            // Nếu đến trang cuối thì quay lại trang đầu, không thì cộng 1
            binding.viewPager.currentItem = (current + 1) % total
        }
    }

    override fun provideBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentHomeBinding.inflate(inflater, container, false)

    override fun setupUI() {
        setupSlider()
        setupFunctionGrid()
        setupAds()
        (requireActivity() as MainActivity).updateStatusBarColor(false)
    }

    private fun setupSlider() {
        val banners = listOf(R.drawable.banner2, R.drawable.banner1)
        binding.viewPager.adapter = SliderAdapter(banners)
        val compositePageTransformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(40))
            addTransformer { page, position ->
                val r = 1 - abs(position)
                page.scaleY = 0.85f + r * 0.15f
            }
        }
        binding.viewPager.setPageTransformer(compositePageTransformer)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 3000)
            }
        })
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

            FunctionType.IMAGES -> {
                startActivity(
                    Intent(requireContext(), GalleryActivity::class.java).apply {
                        putExtra("mode", GalleryMode.SINGLE.name)
                        putExtra("target_tab", GalleryTab.GLOWZA.name)
                    }
                )
            }

            FunctionType.VIDEOS -> {
                startActivity(
                    Intent(requireContext(), GalleryActivity::class.java).apply {
                        putExtra("mode", GalleryMode.SINGLE.name)
                        putExtra("target_tab", GalleryTab.VIDEO.name)
                    }
                )
            }

            else -> {}
        }
    }

    private fun setupAds() {
        val ads = listOf(
            R.drawable.ad1,
            R.drawable.ad2,
            R.drawable.ad3,
            R.drawable.ad4,
            R.drawable.ad5,
            R.drawable.ad6,
            R.drawable.ad7,
            R.drawable.ad8,
            R.drawable.ad9,
        )

        binding.rvAds.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = ImageAdapter(ads)
        }
    }
    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)
        (requireActivity() as MainActivity).updateStatusBarColor(true)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).updateStatusBarColor(false)
        sliderHandler.postDelayed(sliderRunnable, 3000)
    }
}