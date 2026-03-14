package com.sgroupmobile.glowza.ui.camera.fragment

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.view.GestureDetector
import com.sgroupmobile.glowza.ui.camera.CameraController
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.camera.core.CameraSelector
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.databinding.FragmentCameraBinding
import com.sgroupmobile.glowza.ui.camera.CameraSetting
import com.sgroupmobile.glowza.ui.camera.CameraViewModel
import com.sgroupmobile.glowza.ui.camera.FilterAdapter
import com.sgroupmobile.glowza.helper.FilterHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.getValue
import androidx.core.view.isGone

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

        //Setup Adapter cho Filter Menu
        filterAdapter = FilterAdapter { filter ->
            if (filter.id == 0) {
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

        binding.rvFilters.adapter = filterAdapter
        filterAdapter.submitList(filterHelper.loadFilters())

        // Setup CameraController
        cameraController = CameraController(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            previewView = binding.previewView,
            viewModel = cameraViewModel
        )

        // Setup Touch Gestures (Double tap to switch, Pinch to zoom)
        setupGestures()

        binding.root.post {
            cameraController.start()
        }
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(
            requireContext(),
            object: GestureDetector.SimpleOnGestureListener(){
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

        binding.btnFaceFilter.setOnClickListener {
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
            // Flash
            launch {
                cameraViewModel.flash.collect { isFlashOn ->
                    val src = if(isFlashOn) R.drawable.flash else R.drawable.no_flash
                    binding.btnFlash.setImageResource(src)
                }
            }

            //  Ratio màn hình
            launch {
                cameraViewModel.ratio.collect { value ->
                    updatePreviewRatio(value)
                }
            }

            // ML Kit
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

            // Filter
            launch {
                cameraViewModel.selectedFaceFilter.collect { filter ->
                    binding.overlayView.setFilter(filter)
                }
            }

            // Zoom
            launch {
                cameraViewModel.isZoomIn.collect { isZoomIn ->
                    runZoomAnimation(isZoomIn)
                }
            }
        }
    }

    private fun runZoomAnimation(isZoomIn: Boolean) {
        binding.btnZoom.animate()
            .scaleX(0.7f).scaleY(0.7f)
            .setDuration(100)
            .withEndAction {
                val resource = if(!isZoomIn) R.drawable.zoom_in else R.drawable.zoom_out
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

    fun switchAnimate(){
        binding.btnSwitch.animate().cancel()
        binding.btnSwitch.animate().rotationBy(180f).setDuration(400).start()
    }

    private fun startTimerAndTakePhoto(seconds: Int){
        if(seconds <= 0){
            takePhoto()
            return
        }
        binding.tvCountdown.visibility = View.VISIBLE
        object : CountDownTimer(seconds * 1000L, 1000L){
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