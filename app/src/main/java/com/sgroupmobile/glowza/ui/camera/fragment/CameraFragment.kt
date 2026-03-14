package com.sgroupmobile.glowza.ui.camera.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.CountDownTimer
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.databinding.FragmentCameraBinding
import com.sgroupmobile.glowza.helper.FilterHelper
import com.sgroupmobile.glowza.helper.ImageFilterManager
import com.sgroupmobile.glowza.ui.camera.CameraController
import com.sgroupmobile.glowza.ui.camera.CameraSetting
import com.sgroupmobile.glowza.ui.camera.CameraViewModel
import com.sgroupmobile.glowza.ui.camera.FilterAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CameraFragment : BaseFragment<FragmentCameraBinding>() {
    private val cameraViewModel: CameraViewModel by activityViewModels()
    private lateinit var cameraController: CameraController
    private lateinit var gestureDetector: GestureDetector
    private lateinit var filterAdapter: FilterAdapter
    private lateinit var filterHelper: FilterHelper
    private lateinit var scaleGestureDetector: ScaleGestureDetector

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
    }

    override fun initData() {
        filterHelper = FilterHelper(requireContext())

        filterAdapter = FilterAdapter { filter ->
            if (filter.isColorFilter) {
                // Nếu là Image Filter (Chỉnh màu GPUImage)
                cameraViewModel.setSelectedColorFilter(filter.colorCode)
            } else {
                // Nếu là Face Filter (Sticker ML Kit)
                if (filter.id == 0) { // Trường hợp chọn "None"
                    cameraViewModel.setSelectedFaceFilter(null)
                    if (cameraViewModel.isFaceFilterOn.value) {
                        cameraViewModel.toggleFaceFilter()
                    }
                } else {
                    cameraViewModel.setSelectedFaceFilter(filter)
                    if (!cameraViewModel.isFaceFilterOn.value) {
                        cameraViewModel.toggleFaceFilter()
                    }
                }
            }
        }

        binding.rvFilters.adapter = filterAdapter

        // Mặc định nạp dữ liệu Face Filter lúc khởi động
        val allFilters = filterHelper.loadAllFilters()
        cameraViewModel.setFilterTab(isColorTab = false, allFilters)

        // Setup CameraController
        cameraController = CameraController(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            previewView = binding.previewView,
            viewModel = cameraViewModel
        )

        setupGestures()
        binding.root.post { cameraController.start() }
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    switchAnimate()
                    cameraController.switchCamera()
                    return true
                }
            }
        )

        scaleGestureDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val zoomState = cameraController.getZoomState()?.value ?: return false
                    cameraController.setZoomRatio(zoomState.zoomRatio * detector.scaleFactor)
                    return true
                }
            })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setupListeners() {
        binding.btnSwitch.setOnClickListener {
            switchAnimate()
            cameraController.switchCamera()
        }

        binding.previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.btnFlash.setOnClickListener {
            cameraController.toggleFlash()
        }

        // Tab Face Filter
        binding.btnFaceFilter.setOnClickListener {
            cameraViewModel.setFilterTab(false, filterHelper.loadAllFilters())
            updateTabUI(isColor = false)
            toggleFilterMenu()
        }

        // Tab Image Filter (Chỉnh màu)
        binding.btnImageFilter.setOnClickListener {
            cameraViewModel.setFilterTab(true, filterHelper.loadAllFilters())
            updateTabUI(isColor = true)
            toggleFilterMenu()
        }

        binding.btnSetting.setOnClickListener {
            CameraSetting().show(childFragmentManager, "Camera Setting")
        }

        binding.btnSnap.setOnClickListener {
            binding.btnSnap.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                startTimerAndTakePhoto(cameraViewModel.timer.first())
            }
        }

        binding.btnZoom.setOnClickListener {
            val zoomState = cameraController.getZoomState()?.value
            val minZoom = zoomState?.minZoomRatio ?: 1f
            if (cameraViewModel.isZoomIn.value) {
                cameraController.setZoomRatio(2.0f)
            } else {
                cameraController.setZoomRatio(minZoom)
            }
            cameraViewModel.updateZoom()
        }
    }

    private fun updateTabUI(isColor: Boolean) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.primary) // Màu nhấn cho tab đang chọn
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.white)

        binding.btnImageFilter.imageTintList = ColorStateList.valueOf(if (isColor) activeColor else inactiveColor)
        binding.btnFaceFilter.imageTintList = ColorStateList.valueOf(if (isColor) inactiveColor else activeColor)

        // Reset scroll về đầu danh sách và reset vị trí chọn trong adapter
        binding.rvFilters.scrollToPosition(0)
        filterAdapter.resetSelection()
    }

    private fun toggleFilterMenu() {
        if (binding.rvFilters.isGone) {
            binding.rvFilters.visibility = View.VISIBLE
            binding.rvFilters.alpha = 0f
            binding.rvFilters.translationY = 50f
            binding.rvFilters.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        } else {
            binding.rvFilters.animate()
                .alpha(0f)
                .translationY(50f)
                .setDuration(250)
                .withEndAction { binding.rvFilters.visibility = View.GONE }
                .start()
        }
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Flash Observer
            launch {
                cameraViewModel.flash.collect { isFlashOn ->
                    val src = if (isFlashOn) R.drawable.flash else R.drawable.no_flash
                    binding.btnFlash.setImageResource(src)
                }
            }

            // Ratio Observer
            launch {
                cameraViewModel.ratio.collect { value ->
                    updatePreviewRatio(value)
                }
            }

            // ML Kit Faces Observer
            launch {
                cameraViewModel.faces.collect { faces ->
                    binding.overlayView.apply {
                        this.faces = faces
                        this.imageSourceWidth = cameraViewModel.imageSourceWidth
                        this.imageSourceHeight = cameraViewModel.imageSourceHeight
                        this.isFrontCamera = cameraViewModel.cameraFacing.value == CameraSelector.DEFAULT_FRONT_CAMERA
                        invalidate()
                    }
                }
            }

            // Face Filter Observer
            launch {
                cameraViewModel.selectedFaceFilter.collect { filter ->
                    binding.overlayView.setFilter(filter)
                }
            }

            // Tab Filters Observer (Cập nhật danh sách khi đổi Tab)
            launch {
                cameraViewModel.currentTabFilters.collect { list ->
                    filterAdapter.submitList(list)
                }
            }

            // Zoom Observer
            launch {
                cameraViewModel.isZoomIn.collect { isZoomIn ->
                    runZoomAnimation(isZoomIn)
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                cameraViewModel.selectedColorFilter.collect { colorCode ->
                    applyColorOverlay(colorCode)
                }
            }
        }
    }

    private fun applyColorOverlay(code: String) {
        val overlay = binding.viewColorFilterOverlay

        when (code.uppercase()) {
            "PASTEL" -> {
                overlay.setBackgroundColor(Color.parseColor("#40FFC0CB")) // Hồng nhạt 25% alpha
                overlay.alpha = 1.0f
            }
            "VINTAGE" -> {
                overlay.setBackgroundColor(Color.parseColor("#408B4513")) // Nâu cổ điển 25% alpha
                overlay.alpha = 1.0f
            }
            "GRAYSCALE" -> {
                overlay.setBackgroundColor(Color.BLACK)
                overlay.alpha = 0.2f
            }
            else -> {
                overlay.alpha = 0.0f
            }
        }
    }
    private fun runZoomAnimation(isZoomIn: Boolean) {
        binding.btnZoom.animate()
            .scaleX(0.7f).scaleY(0.7f)
            .setDuration(100)
            .withEndAction {
                val resource = if (!isZoomIn) R.drawable.zoom_in else R.drawable.zoom_out
                binding.btnZoom.setImageResource(resource)
                binding.btnZoom.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraController.stop()
    }

    private fun takePhoto() {
        viewLifecycleOwner.lifecycleScope.launch {
            val isBackCamera = cameraViewModel.cameraFacing.first() == CameraSelector.DEFAULT_BACK_CAMERA
            val isFlashOn = cameraViewModel.flash.first()

            if (!isBackCamera && isFlashOn) {
                binding.viewFlashEffect.visibility = View.VISIBLE
                binding.viewFlashEffect.alpha = 1f
                kotlinx.coroutines.delay(100)
            } else {
                runFlashEffect()
            }

            android.media.MediaActionSound().play(android.media.MediaActionSound.SHUTTER_CLICK)

            cameraController.takePhoto { uri ->
                binding.viewFlashEffect.visibility = View.GONE
                navigateToPreview(uri)
                binding.btnSnap.isEnabled = true
            }
        }
    }

    private fun navigateToPreview(uri: android.net.Uri) {
        val previewFragment = ImagePreviewFragment().apply {
            arguments = bundleOf("imageUri" to uri.toString())
        }
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragment_container_view, previewFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updatePreviewRatio(ratio: String) {
        val constraintLayout = binding.root
        val constraintSet = androidx.constraintlayout.widget.ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.setDimensionRatio(binding.frameCameraPreview.id, ratio)

        val transition = androidx.transition.ChangeBounds().apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : androidx.transition.TransitionListenerAdapter() {
                override fun onTransitionEnd(transition: androidx.transition.Transition) {
                    cameraController.bindUseCases(cameraViewModel.cameraFacing.value, cameraViewModel.isFaceFilterOn.value)
                }
            })
        }

        androidx.transition.TransitionManager.beginDelayedTransition(constraintLayout, transition)
        constraintSet.applyTo(constraintLayout)
    }

    fun switchAnimate() {
        binding.btnSwitch.animate().cancel()
        binding.btnSwitch.animate().rotationBy(180f).setDuration(400).start()
    }

    private fun startTimerAndTakePhoto(seconds: Int) {
        if (seconds <= 0) {
            takePhoto()
            return
        }
        binding.tvCountdown.visibility = View.VISIBLE
        object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000 + 1
                binding.tvCountdown.text = secondsLeft.toString()
                binding.tvCountdown.animate().scaleX(1.5f).scaleY(1.5f).setDuration(0).withEndAction {
                    binding.tvCountdown.animate().scaleX(1f).scaleY(1f).setDuration(500).start()
                }.start()
            }

            override fun onFinish() {
                binding.tvCountdown.visibility = View.GONE
                takePhoto()
            }
        }.start()
    }

    private fun runFlashEffect() {
        binding.viewFlashEffect.apply {
            visibility = View.VISIBLE
            alpha = 1f
            animate().alpha(0f).setDuration(300).withEndAction { visibility = View.GONE }.start()
        }
    }
}