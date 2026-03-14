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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class CameraFragment : BaseFragment<FragmentCameraBinding>() {
    private val cameraViewModel: CameraViewModel by activityViewModels()
    private lateinit var cameraController: CameraController
    private lateinit var gestureDetector: GestureDetector

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
        cameraController = CameraController(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            previewView = binding.previewView,
            viewModel = cameraViewModel
        )

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

                    val currentZoomRatio = zoomState.zoomRatio
                    val delta = detector.scaleFactor

                    cameraController.setZoomRatio(currentZoomRatio * delta)
                    return true
                }
            })
        binding.root.post {
            cameraController.start()
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setupListeners() {
        binding.btnSwitch.setOnClickListener {
            switchAnimate()
            cameraController.switchCamera()
        }

        binding.previewView.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.btnFlash.setOnClickListener {
            cameraController.toggleFlash()
        }

        binding.btnExit.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnSetting.setOnClickListener {
            val cameraSetting = CameraSetting()
            cameraSetting.show(childFragmentManager, "Camera Setting")
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
    fun switchAnimate(){
        binding.btnSwitch.animate().cancel()
        binding.btnSwitch.animate()
            .rotationBy(180f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(400)
            .start()
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                cameraViewModel.flash.collect { isFlashOn ->
                    val src = if(isFlashOn) R.drawable.flash else R.drawable.no_flash
                    binding.btnFlash.setImageResource(src)
                }
            }

            launch {
                cameraViewModel.ratio.collect { value ->
                    updatePreviewRatio(value)
                    binding.frameCameraPreview.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                        override fun onLayoutChange(
                            v: View?, left: Int, top: Int, right: Int, bottom: Int,
                            oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                        ) {
                            binding.frameCameraPreview.removeOnLayoutChangeListener(this)
                        }
                    })
                }
            }

            launch {
                cameraViewModel.isZoomIn.collect { isZoomIn ->
                    binding.btnZoom.animate()
                        .scaleX(0.7f)
                        .scaleY(0.7f)
                        .setDuration(100)
                        .withEndAction {
                            val resource = if(!isZoomIn) R.drawable.zoom_in else R.drawable.zoom_out
                            binding.btnZoom.setImageResource(resource)

                            binding.btnZoom.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .start()
                        }
                        .start()
                }
            }
            launch {
                cameraViewModel.grid.collect { isGridOn ->
                    binding.overlayView.isGridOn = isGridOn
                    binding.overlayView.invalidate()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraController.stop()
    }

    private fun startTimerAndTakePhoto(seconds: Int){
        if(seconds <= 0){
            takePhoto()
            binding.btnSnap.isEnabled = true
            return
        }
        binding.tvCountdown.visibility = View.VISIBLE
        object : CountDownTimer(seconds * 1000L, 1000L){
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000 + 1
                binding.tvCountdown.text = secondsLeft.toString()
                binding.tvCountdown.scaleX = 1.5f
                binding.tvCountdown.scaleY = 1.5f
                binding.tvCountdown.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .start()
            }

            override fun onFinish() {
                binding.tvCountdown.visibility = View.GONE
                binding.btnSnap.isEnabled = true
                takePhoto()
            }
        }.start()
    }
    private fun takePhoto() {
        viewLifecycleOwner.lifecycleScope.launch {
            val isBackCamera = cameraViewModel.cameraFacing.first() == CameraSelector.DEFAULT_BACK_CAMERA
            val isFlashOn = cameraViewModel.flash.first()
            if (!isBackCamera && isFlashOn) {
                // Hiện màn hình trắng xóa
                binding.viewFlashEffect.visibility = View.VISIBLE
                binding.viewFlashEffect.alpha = 1f

                // Đợi 100ms để màn hình kịp sáng lên rồi mới phát âm thanh và chụp
                kotlinx.coroutines.delay(100)
            } else {
                // Nếu là camera sau thì chỉ nháy Flash hiệu ứng bình thường
                runFlashEffect()
            }

            val sound = android.media.MediaActionSound()
            sound.play(android.media.MediaActionSound.SHUTTER_CLICK)

            cameraController.takePhoto { uri ->
                binding.viewFlashEffect.visibility = View.GONE
                navigateToPreview(uri)
            }
        }
    }

    private fun navigateToPreview(uri: android.net.Uri) {
        val previewFragment = ImagePreviewFragment().apply {
            arguments = bundleOf("imageUri" to uri.toString())
        }
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragment_container_view, previewFragment)
            .addToBackStack(null)
            .commit()
    }
    private fun runFlashEffect() {
        binding.viewFlashEffect.apply {
            visibility = View.VISIBLE
            alpha = 1f

            animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    visibility = View.GONE
                }
                .start()
        }
    }
    private fun updatePreviewRatio(ratio: String) {
        val constraintLayout = binding.root
        val constraintSet = androidx.constraintlayout.widget.ConstraintSet()
        constraintSet.clone(constraintLayout)

        constraintSet.setDimensionRatio(binding.frameCameraPreview.id, ratio)

        constraintSet.constrainHeight(binding.viewTopLetterbox.id, androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(binding.viewBottomLetterbox.id, androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT)

        val transition = androidx.transition.ChangeBounds()
        transition.duration = 400
        transition.interpolator = AccelerateDecelerateInterpolator()

        transition.addListener(object : androidx.transition.TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: androidx.transition.Transition) {
                binding.root.requestLayout()
                cameraController.bindUseCases(cameraViewModel.cameraFacing.value)
            }
        })

        androidx.transition.TransitionManager.beginDelayedTransition(constraintLayout, transition)
        constraintSet.applyTo(constraintLayout)
    }
}

