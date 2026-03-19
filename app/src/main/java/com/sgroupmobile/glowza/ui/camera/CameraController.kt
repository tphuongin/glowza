package com.sgroupmobile.glowza.ui.camera

import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.util.Log
import android.util.Rational
import androidx.camera.core.Camera
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.ViewPort.FILL_CENTER
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.sgroupmobile.glowza.common.enums.CameraMode
import com.sgroupmobile.glowza.data.model.AppFilter
import com.sgroupmobile.glowza.helper.ImageFilterManager
import jp.co.cyberagent.android.gpuimage.GPUImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
    private var videoCapture: VideoCapture<Recorder>? = null
    private var camera: Camera? = null
    private var recording: Recording? = null
    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }
    fun start() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            lifecycleOwner.lifecycleScope.launch {
                viewModel.cameraFacing.collect { selector ->
                    bindUseCases(selector, viewModel.isFaceFilterOn.value, viewModel.cameraMode.value)
                }
            }
            lifecycleOwner.lifecycleScope.launch {
                viewModel.flash.collect { isFlashOn ->
                    imageCapture?.flashMode = if(isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                }
            }
            lifecycleOwner.lifecycleScope.launch {
                viewModel.isFaceFilterOn.collect { isOn ->
                    bindUseCases(viewModel.cameraFacing.value, isOn, viewModel.cameraMode.value)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun bindUseCases(selector: CameraSelector, isFilterOn: Boolean, mode: CameraMode) {
        val provider = cameraProvider ?: return
        provider.unbindAll()
        imageCapture = null
        videoCapture = null

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val useCaseGroupBuilder = UseCaseGroup.Builder()
            .addUseCase(preview)
        if(mode == CameraMode.VIDEO){
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            if (videoCapture != null) {
                useCaseGroupBuilder.addUseCase(videoCapture!!)
            }
        } else{
            imageCapture = ImageCapture.Builder()
                .setTargetRotation(previewView.display.rotation)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            useCaseGroupBuilder.addUseCase(imageCapture!!)
        }

        if (isFilterOn) {
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(previewView.display.rotation)
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }
            useCaseGroupBuilder.addUseCase(imageAnalyzer)
        } else {
            viewModel.updateFaces(emptyList(), 0, 0)
        }

        val screenAspectRatio = Rational(previewView.width, previewView.height)
        val viewPort = ViewPort.Builder(screenAspectRatio, previewView.display.rotation)
            .setScaleType(FILL_CENTER)
            .build()
        useCaseGroupBuilder.setViewPort(viewPort)

        try {
            camera = provider.bindToLifecycle(lifecycleOwner, selector, useCaseGroupBuilder.build()) as Camera?
        } catch (e: Exception) {
            Log.e("CameraController", "Binding failed", e)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val isRotated = imageProxy.imageInfo.rotationDegrees % 180 != 0
                val width = if (isRotated) imageProxy.height else imageProxy.width
                val height = if (isRotated) imageProxy.width else imageProxy.height
                viewModel.updateFaces(faces, width, height)
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun takePhoto(onImageCaptured: (Uri) -> Unit) {
        val imageCapture = imageCapture ?: return
        val photoFile = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        // Load ảnh gốc
                        var bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                        val isFrontCamera = viewModel.cameraFacing.value == CameraSelector.DEFAULT_FRONT_CAMERA

                        // Lật ảnh nếu là camera trước (Selfie)
                        if (isFrontCamera) {
                            bitmap = flipBitmap(bitmap)
                        }

                        // Áp dụng Image Filter (GPUImage)
                        val colorCode = viewModel.selectedColorFilter.value
                        if (colorCode.isNotEmpty()) {
                            bitmap = applyGPUFilter(bitmap, colorCode)
                        }

                        // Áp dụng Face Filter (Overlay)
                        val selectedFace = viewModel.selectedFaceFilter.value
                        val detectedFaces = viewModel.faces.value
                        if (selectedFace != null && detectedFaces.isNotEmpty()) {
                            bitmap = mergeFaceFilterToBitmap(bitmap, selectedFace, detectedFaces)
                        }

                        // Lưu đè lại file
                        saveBitmapToFile(bitmap, photoFile)
                        bitmap.recycle()

                        withContext(Dispatchers.Main) {
                            onImageCaptured(Uri.fromFile(photoFile))
                        }
                    }
                }
                override fun onError(e: ImageCaptureException) { Log.e("Camera", "Failed", e) }
            }
        )
    }

    private fun applyGPUFilter(bitmap: Bitmap, code: String): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setFilter(ImageFilterManager.getGPUFilter(code))
        return gpuImage.getBitmapWithFilterApplied(bitmap)
    }

    private fun mergeFaceFilterToBitmap(original: Bitmap, filter: AppFilter, faces: List<Face>): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        FilterPainter.drawFiltersOnCanvas(
            canvas, original.width, original.height,
            viewModel.imageSourceWidth, viewModel.imageSourceHeight,
            faces, filter, context,
            isFrontCamera = true
        )

        return result
    }

    fun setTorch(isOn: Boolean) {
        camera?.cameraControl?.enableTorch(isOn)
    }
    fun toggleRecording(onVideoEvent: (VideoRecordEvent) -> Unit) {
        val currentRecording = recording
        if (currentRecording != null) {
            currentRecording.stop()
            recording = null
            return
        }

        val name = "Glowza_Video_${System.currentTimeMillis()}.mp4"
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture?.output
            ?.prepareRecording(context, mediaStoreOutputOptions)
            ?.apply {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }
            ?.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Status -> {
                        // Tính toán thời gian đã trôi qua
                        val stats = recordEvent.recordingStats
                        val timeInSeconds = stats.recordedDurationNanos / 1_000_000_000
                        onVideoEvent(recordEvent)
                    }
                    else -> onVideoEvent(recordEvent)
                }
            }
    }

    private fun flipBitmap(source: Bitmap): Bitmap {
        val matrix = Matrix().apply { postScale(-1f, 1f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    }

    fun stop() {
        faceDetector.close()
        camera = null
    }

    fun setZoomRatio(value: Float) {
        val zoomState = getZoomState()?.value
        camera?.cameraControl?.setZoomRatio(value.coerceIn(zoomState?.minZoomRatio ?: 1f, zoomState?.maxZoomRatio ?: 10f))
    }

    fun smoothZoom(targetZoom: Float){
        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return
        val currentZoom = zoomState.zoomRatio
        val animator = ValueAnimator.ofFloat(currentZoom, targetZoom)
        animator.duration = 400
        animator.addUpdateListener {
            setZoomRatio(it.animatedValue as Float)
        }
        animator.start()
    }

    fun getZoomState() = camera?.cameraInfo?.zoomState
    fun toggleFlash() = viewModel.toggleFlash()
    fun switchCamera() = viewModel.switchCamera()
}