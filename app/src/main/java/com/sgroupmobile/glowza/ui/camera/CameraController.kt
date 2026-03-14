package com.sgroupmobile.glowza.ui.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Rational
import android.view.GestureDetector
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.camera.core.*
import androidx.camera.core.ViewPort.FILL_CENTER
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sgroupmobile.glowza.common.enum.CameraRatio
import kotlinx.coroutines.launch
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
                    bindUseCases(selector)
                }
            }
            lifecycleOwner.lifecycleScope.launch {
                viewModel.flash.collect { isFlashOn ->
                    imageCapture?.flashMode = if(isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                }
            }

        }, ContextCompat.getMainExecutor(context))
    }
    fun bindUseCases(selector: CameraSelector) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val screenAspectRatio = Rational(previewView.width, previewView.height)

        val viewPort = ViewPort.Builder(screenAspectRatio, previewView.display.rotation)
            .setScaleType(FILL_CENTER)
            .build()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(previewView.display.rotation) //Set về đúng hướng cầm máy của người dùng
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageCapture!!)
            .setViewPort(viewPort)
            .build()

        try {
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                useCaseGroup
            )
        } catch (e: Exception) {
            Log.e("CameraController", "Binding failed", e)
        }
    }

    fun stop() {
        orientationEventListener.disable()
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