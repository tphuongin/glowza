package com.sgroupmobile.glowza.ui.camera.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.media.MediaActionSound
import android.os.Bundle
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.common.enums.CameraMode
import com.sgroupmobile.glowza.common.enums.CameraTimer
import com.sgroupmobile.glowza.data.model.AppFilter
import com.sgroupmobile.glowza.databinding.FragmentCameraBinding
import com.sgroupmobile.glowza.helper.FilterHelper
import com.sgroupmobile.glowza.helper.PermissionHelper
import com.sgroupmobile.glowza.ui.camera.CameraController
import com.sgroupmobile.glowza.ui.camera.CameraSetting
import com.sgroupmobile.glowza.ui.camera.CameraViewModel
import com.sgroupmobile.glowza.ui.camera.adapter.FilterAdapter
import com.sgroupmobile.glowza.ui.camera.adapter.ModeAdapter
import com.sgroupmobile.glowza.util.CustomAnimation
import com.sgroupmobile.glowza.util.CustomAnimation.toggleViewSmooth
import com.sgroupmobile.glowza.util.showFancySnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.view.isVisible

@AndroidEntryPoint
class CameraFragment : BaseFragment<FragmentCameraBinding>() {
    private val cameraViewModel: CameraViewModel by activityViewModels()
    private lateinit var cameraController: CameraController
    private lateinit var gestureDetector: GestureDetector
    private lateinit var filterAdapter: FilterAdapter
    private lateinit var filterHelper: FilterHelper
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var hideRunnable: Runnable? = null
    private lateinit var permissionHelper: PermissionHelper
    private val snapHelper by lazy { LinearSnapHelper() }
    private val cameraSound by lazy { MediaActionSound() }
    private val modes: List<CameraMode> by lazy {
        CameraMode.entries
    }
    private val modeAdapter by lazy {
        ModeAdapter(modes){ position ->
            smoothScrollSlow(position)
        }
    }
    private var isRecording = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = PermissionHelper(requireActivity() as AppCompatActivity)
    }

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
        binding.rvModeCamera.layoutManager = LinearLayoutManager(requireContext(),
            LinearLayoutManager.HORIZONTAL, false)
        snapHelper.attachToRecyclerView(binding.rvModeCamera)
        val recyclerWidth = Resources.getSystem().displayMetrics.widthPixels
        val itemWidth = resources.getDimensionPixelSize(R.dimen.mode_width)

        val padding = (recyclerWidth - itemWidth) / 2

        binding.rvModeCamera.setPadding(padding, 0, padding, 0)
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

        binding.rvModeCamera.adapter = modeAdapter

        val allFilters = emptyMap<String, List<AppFilter>>()
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
                    val value = zoomState.zoomRatio * detector.scaleFactor
                    cameraController.setZoomRatio(value)
                    if((value == zoomState.minZoomRatio && !cameraViewModel.isZoomIn.value)
                        || (value == zoomState.maxZoomRatio && cameraViewModel.isZoomIn.value))
                        cameraViewModel.updateZoom()
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
            filterAdapter.resetSelection()
            val forceOpen = cameraViewModel.currentTabFilters.value !=
            cameraViewModel.setFilterTab(false, filterHelper.loadAllFilters())
            toggleFilterMenu(isColor = false)
        }

        // Tab Image Filter (Chỉnh màu)
        binding.btnImageFilter.setOnClickListener {
            filterAdapter.resetSelection()
            cameraViewModel.setFilterTab(true, filterHelper.loadAllFilters())
            toggleFilterMenu(isColor = true)
        }

        binding.btnSetting.setOnClickListener {
            CameraSetting().show(childFragmentManager, "Camera Setting")
        }

        binding.btnSnap.setOnClickListener {
            val mode = cameraViewModel.cameraMode.value
            if (mode == CameraMode.VIDEO) {
                permissionHelper.requestPermission(
                    android.Manifest.permission.RECORD_AUDIO,
                    R.layout.dialog_permission_audio
                ) {
                    if (isRecording) {
                        handleVideoRecording()
                    } else {
                        startTimerAndCapture(cameraViewModel.timer.value)
                    }
                }
            } else {
                binding.btnSnap.isEnabled = false
                startTimerAndCapture(cameraViewModel.timer.value)
            }
        }

        binding.btnZoom.setOnClickListener {
            val zoomState = cameraController.getZoomState()?.value
            val minZoom = zoomState?.minZoomRatio ?: 1f
            if (cameraViewModel.isZoomIn.value) {
                cameraController.smoothZoom(2.0f)
            } else {
                cameraController.smoothZoom(minZoom)
            }
            cameraViewModel.updateZoom()
        }

        binding.rvModeCamera.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if(newState == RecyclerView.SCROLL_STATE_IDLE){
                    val layoutManager = recyclerView.layoutManager ?: return
                    val snapView = snapHelper.findSnapView(layoutManager)
                    snapView?.let {
                        val position = layoutManager.getPosition(snapView)
                        onModeItemSelected(position)
                    }
                }
            }
        })
    }
    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    private fun onModeItemSelected(position: Int){
        modeAdapter.updateSelectedPosition(position)
        cameraViewModel.updateCameraMode(modes[position])
    }
    private fun toggleFilterMenu(isColor: Boolean) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.primary) // Màu nhấn cho tab đang chọn
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.white)

        // Reset scroll về đầu danh sách và reset vị trí chọn trong adapter
        binding.rvFilters.scrollToPosition(0)
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
            binding.btnImageFilter.imageTintList = ColorStateList.valueOf(if (isColor) activeColor else inactiveColor)
            binding.btnFaceFilter.imageTintList = ColorStateList.valueOf(if (isColor) inactiveColor else activeColor)
        } else {
            binding.rvFilters.animate()
                .alpha(0f)
                .translationY(50f)
                .setDuration(250)
                .withEndAction { binding.rvFilters.visibility = View.GONE }
                .start()
            binding.btnImageFilter.imageTintList = ColorStateList.valueOf(inactiveColor)
            binding.btnFaceFilter.imageTintList = ColorStateList.valueOf(inactiveColor)
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

            launch {
                cameraViewModel.timer.collect {
                    var string = it.toString()
                    if(string != CameraTimer.TIMER_OFF.time.toString())
                        string += "s"
                    else string = "OFF"
                    binding.tvTimer.text = string
                    textAnimate(binding.tvTimer)
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

            launch {
                cameraViewModel.cameraMode.collect { mode ->
                    val isVideoMode = mode == CameraMode.VIDEO

                    if (isVideoMode) {
                        if (cameraViewModel.isFaceFilterOn.value) {
                            cameraViewModel.toggleFaceFilter()
                        }
                        cameraViewModel.setSelectedColorFilter("")

                        if (binding.rvFilters.isVisible) {
                            toggleViewSmooth(binding.rvFilters, false)
                        }
                        binding.overlayView.setFilter(null)
                        binding.overlayView.invalidate()
                    }

                    transitionWithSmoothEffect {
                        cameraController.bindUseCases(
                            cameraViewModel.cameraFacing.value,
                            cameraViewModel.isFaceFilterOn.value,
                            mode
                        )
                    }

                    updateCaptureButtonUI(mode)
                    updateChangeModeUI()
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

            launch {
                cameraViewModel.selectedColorFilter.collect { colorCode ->
                    applyColorOverlay(colorCode)
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
    private fun handleVideoRecording() {
        cameraController.toggleRecording { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    isRecording = true
                    cameraSound.play(MediaActionSound.START_VIDEO_RECORDING)
                    if (cameraViewModel.flash.value) {
                        cameraController.setTorch(true)
                    }
                    binding.btnSnap.setImageResource(R.drawable.stop_video)
                    binding.tvVideoTimer.visibility = View.VISIBLE
                }
                is VideoRecordEvent.Status -> {
                    val duration = event.recordingStats.recordedDurationNanos / 1_000_000_000
                    binding.tvVideoTimer.text = formatDuration(duration)
                }
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    cameraSound.play(MediaActionSound.STOP_VIDEO_RECORDING)
                    cameraController.setTorch(false)
                    binding.tvVideoTimer.visibility = View.GONE
                    updateCaptureButtonUI(CameraMode.VIDEO)

                    if (!event.hasError()) {
                        showFancySnackbar(getString(R.string.save_success), binding.root)
                    }
                }
            }
        }
    }
    private fun updateCaptureButtonUI(mode: CameraMode){
        if (mode == CameraMode.PHOTO){
            binding.btnSnap.setImageResource(R.drawable.snap)
        } else{
            binding.btnSnap.setImageResource(R.drawable.video_snap)
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
    private fun transitionWithSmoothEffect(action: () -> Unit) {
        val bitmap = binding.previewView.bitmap
        if (bitmap != null) {
            binding.ivTransitionMask.setImageBitmap(bitmap)
            binding.ivTransitionMask.visibility = View.VISIBLE
            binding.ivTransitionMask.alpha = 1f
        }

        action.invoke()

        binding.ivTransitionMask.postDelayed({
            binding.ivTransitionMask.animate()
                .alpha(0f)
                .setDuration(400)
                .withEndAction {
                    binding.ivTransitionMask.visibility = View.GONE
                }
                .start()
        }, 300)
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
        hideRunnable?.let { binding.tvTimer.removeCallbacks(it) }
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

            cameraSound.play(MediaActionSound.SHUTTER_CLICK)

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
                    cameraController.bindUseCases(cameraViewModel.cameraFacing.value, cameraViewModel.isFaceFilterOn.value, cameraViewModel.cameraMode.value)
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

    private fun startTimerAndCapture(seconds: Int) {
        if (seconds <= 0) {
            executeCaptureByMode()
            return
        }

        binding.tvCountdown.visibility = View.VISIBLE
        object : CountDownTimer(seconds * 1000L + 100L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                binding.btnSnap.isEnabled = false
                val secondsLeft = (millisUntilFinished / 1000).toInt()

                if (secondsLeft <= 0) {
                    binding.tvCountdown.visibility = View.GONE
                } else {
                    binding.tvCountdown.text = secondsLeft.toString()

                    binding.tvCountdown.animate()
                        .scaleX(1.5f).scaleY(1.5f)
                        .setDuration(0)
                        .withEndAction {
                            binding.tvCountdown.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(500)
                                .start()
                        }.start()
                }
            }

            override fun onFinish() {
                binding.tvCountdown.visibility = View.GONE
                executeCaptureByMode()
            }
        }.start()
    }

    private fun executeCaptureByMode() {
        if (cameraViewModel.cameraMode.value == CameraMode.PHOTO) {
            takePhoto()

        } else {
            handleVideoRecording()
            binding.btnSnap.isEnabled = true
        }
    }
    private fun updateChangeModeUI() {
        val isVideoMode = cameraViewModel.cameraMode.value == CameraMode.VIDEO
        val isFrontCamera = cameraViewModel.cameraFacing.value == CameraSelector.DEFAULT_FRONT_CAMERA
        //Flash
        val shouldShowFlash = !(isVideoMode && isFrontCamera)
        CustomAnimation.toggleViewSmooth(binding.btnFlash, shouldShowFlash)

        // Filter
        val shouldShowFilters = !isVideoMode
        CustomAnimation.toggleViewSmooth(binding.btnFaceFilter, shouldShowFilters)
        CustomAnimation.toggleViewSmooth(binding.btnImageFilter, shouldShowFilters)

        if (isVideoMode && !binding.rvFilters.isGone) {
            CustomAnimation.toggleViewSmooth(binding.rvFilters, false)
        }
    }

    private fun runFlashEffect() {
        binding.viewFlashEffect.apply {
            visibility = View.VISIBLE
            alpha = 1f
            animate().alpha(0f).setDuration(300).withEndAction { visibility = View.GONE }.start()
        }
    }
    private fun textAnimate(view: View) {

        view.post {

            view.animate().cancel()
            hideRunnable?.let { view.removeCallbacks(it) }

            view.visibility = View.VISIBLE

            val parentWidth = (view.parent as View).width
            val centerX = (parentWidth - view.width) / 2f

            view.x = parentWidth.toFloat()

            view.animate()
                .x(centerX)
                .setDuration(300)
                .start()

            hideRunnable = Runnable {
                view.visibility = View.GONE
            }

            view.postDelayed(hideRunnable!!, 3000)
        }
    }
    fun smoothScrollSlow(position: Int) {
        val layoutManager = binding.rvModeCamera.layoutManager as LinearLayoutManager

        val smoothScroller = object : LinearSmoothScroller(binding.rvModeCamera.context) {

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 100f / displayMetrics.densityDpi
            }

            override fun getHorizontalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }

        smoothScroller.targetPosition = position
        layoutManager.startSmoothScroll(smoothScroller)
    }
}
