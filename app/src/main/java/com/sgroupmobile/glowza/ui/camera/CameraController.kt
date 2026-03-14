package com.sgroupmobile.glowza.ui.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Rational
import android.view.OrientationEventListener
import android.view.Surface
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.core.ViewPort.FILL_CENTER
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

class CameraController @Inject constructor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val viewModel: CameraViewModel
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }
    private var imageAnalyzer: ImageAnalysis? = null
    private val orientationEventListener by lazy {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                // Chuyển đổi góc xoay từ độ sang hằng số Surface.ROTATION
                val rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageCapture?.targetRotation = rotation
            }
        }
    }

    fun start() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        orientationEventListener.enable()
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            lifecycleOwner.lifecycleScope.launch {
                viewModel.cameraFacing.collect { selector ->
                    bindUseCases(selector, viewModel.isFaceFilterOn.value)
                }
            }
            lifecycleOwner.lifecycleScope.launch {
                viewModel.flash.collect { isFlashOn ->
                    imageCapture?.flashMode = if(isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                }
            }
            lifecycleOwner.lifecycleScope.launch {
                viewModel.isFaceFilterOn.collect { isOn ->
                    bindUseCases(viewModel.cameraFacing.value, isOn)
                }
            }

        }, ContextCompat.getMainExecutor(context))
    }
    fun bindUseCases(selector: CameraSelector, isFilterOn: Boolean) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(previewView.display.rotation) //Set về đúng hướng cầm máy của người dùng
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val useCaseGroupBuilder = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageCapture!!)

        if (isFilterOn) {
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(previewView.display.rotation)
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }
            useCaseGroupBuilder.addUseCase(imageAnalyzer!!)
        } else {
            viewModel.updateFaces(emptyList(), 0, 0)
        }

        val screenAspectRatio = Rational(previewView.width, previewView.height)
        val viewPort = ViewPort.Builder(screenAspectRatio, previewView.display.rotation)
            .setScaleType(FILL_CENTER)
            .build()
        useCaseGroupBuilder.setViewPort(viewPort)

        try {
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                useCaseGroupBuilder.build()
            )
        } catch (e: Exception) {
            Log.e("CameraController", "Binding failed", e)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    // Tính toán kích thước frame để Overlay vẽ chính xác
                    val isRotated = imageProxy.imageInfo.rotationDegrees % 180 != 0
                    val width = if (isRotated) imageProxy.height else imageProxy.width
                    val height = if (isRotated) imageProxy.width else imageProxy.height

                    viewModel.updateFaces(faces, width, height)
                }
                .addOnFailureListener { e ->
                    Log.e("MLKit", "Face detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
    fun stop() {
        orientationEventListener.disable()
        faceDetector.close()
        camera = null
    }

    fun toggleFlash() {
        viewModel.toggleFlash()
    }
    fun switchCamera(){
        viewModel.switchCamera()
    }
    fun takePhoto(onImageCaptured: (Uri) -> Unit) {
        val imageCapture = imageCapture ?: return
        val photoFile = java.io.File(context.cacheDir, "preview_photo.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    lifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val isFrontCamera = viewModel.cameraFacing.value == CameraSelector.DEFAULT_FRONT_CAMERA

                        if (isFrontCamera) {
                            flipImage(photoFile)
                        }

                        launch(kotlinx.coroutines.Dispatchers.Main) {
                            onImageCaptured(Uri.fromFile(photoFile))
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraController", "Chụp thất bại", exception)
                }
            }
        )
    }

    private fun flipImage(file: java.io.File) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            val matrix = android.graphics.Matrix()

            // Lật ngược theo trục X (ngang)
            matrix.preScale(-1.0f, 1.0f)

            val flippedBitmap = android.graphics.Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            // Ghi đè lại vào file cũ
            java.io.FileOutputStream(file).use { out ->
                flippedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
            }

            // Giải phóng bộ nhớ
            bitmap.recycle()
            flippedBitmap.recycle()
        } catch (e: Exception) {
            Log.e("CameraController", "Lỗi lật ảnh", e)
        }
    }
    fun setZoomRatio(value: Float) {
        val min = getZoomState()?.value?.minZoomRatio ?: 1f
        val max = getZoomState()?.value?.maxZoomRatio ?: 10f

        val clampedValue = value.coerceIn(min, max)
        camera?.cameraControl?.setZoomRatio(clampedValue)
    }
    fun getZoomState() = camera?.cameraInfo?.zoomState

}