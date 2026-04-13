package com.example.privaro.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class ScanResult(
    val totalPeopleCount: Int,
    val frontPeopleCount: Int,
    val backPeopleCount: Int,
    val frontConfidence: Float,
    val backConfidence: Float
)

class SurroundingScanner(private val context: Context) {

    companion object {
        private const val TAG = "SurroundingScanner"
        private const val FRAMES_TO_CAPTURE = 3
        private const val CAPTURE_DELAY_MS = 300L
    }

    private val personDetector = PersonDetector()
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private var cameraProvider: ProcessCameraProvider? = null
    private var isScanning = false
    private var scanJob: Job? = null

    private val lifecycleOwner = object : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)

        init {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        fun start() {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }

    fun startScan(onComplete: (ScanResult) -> Unit) {
        if (isScanning) {
            Log.w(TAG, "Scan already in progress")
            return
        }

        isScanning = true
        Log.d(TAG, "Starting surrounding scan")

        scanJob = coroutineScope.launch {
            try {
                val provider = getCameraProvider()
                cameraProvider = provider

                lifecycleOwner.start()

                // Scan back camera first
                Log.d(TAG, "Scanning back camera...")
                val backResult = scanCamera(provider, CameraSelector.DEFAULT_BACK_CAMERA, CameraSource.BACK)

                // Small delay between cameras
                kotlinx.coroutines.delay(500)

                // Then scan front camera
                Log.d(TAG, "Scanning front camera...")
                val frontResult = scanCamera(provider, CameraSelector.DEFAULT_FRONT_CAMERA, CameraSource.FRONT)

                // Combine results
                val totalResult = ScanResult(
                    totalPeopleCount = backResult.personCount + frontResult.personCount,
                    frontPeopleCount = frontResult.personCount,
                    backPeopleCount = backResult.personCount,
                    frontConfidence = frontResult.confidence,
                    backConfidence = backResult.confidence
                )

                Log.d(TAG, "Scan complete: ${totalResult.totalPeopleCount} people detected")
                onComplete(totalResult)

            } catch (e: Exception) {
                Log.e(TAG, "Error during scan", e)
                onComplete(ScanResult(0, 0, 0, 0f, 0f))
            } finally {
                isScanning = false
                stopScan()
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private suspend fun scanCamera(
        provider: ProcessCameraProvider,
        cameraSelector: CameraSelector,
        source: CameraSource
    ): PersonDetectionResult = withContext(Dispatchers.Main) {
        var result = PersonDetectionResult(0, 0f, source)
        var framesProcessed = 0
        var maxPeopleCount = 0
        var maxConfidence = 0f

        try {
            // Unbind any existing use cases
            provider.unbindAll()

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            var captureComplete = false
            val completionLock = Object()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (framesProcessed >= FRAMES_TO_CAPTURE || captureComplete) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                coroutineScope.launch {
                    try {
                        val bitmap = imageProxyToBitmap(imageProxy)
                        if (bitmap != null) {
                            val detection = personDetector.detectPeople(bitmap, source)

                            if (detection.personCount > maxPeopleCount) {
                                maxPeopleCount = detection.personCount
                            }
                            if (detection.confidence > maxConfidence) {
                                maxConfidence = detection.confidence
                            }

                            framesProcessed++
                            Log.d(TAG, "Frame $framesProcessed/$FRAMES_TO_CAPTURE processed from $source")

                            bitmap.recycle()

                            if (framesProcessed >= FRAMES_TO_CAPTURE) {
                                captureComplete = true
                                result = PersonDetectionResult(maxPeopleCount, maxConfidence, source)
                                synchronized(completionLock) {
                                    completionLock.notifyAll()
                                }
                            }
                        }
                    } finally {
                        imageProxy.close()
                    }
                }
            }

            // Bind the camera
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )

            // Wait for frames to be captured (with timeout)
            withContext(Dispatchers.IO) {
                synchronized(completionLock) {
                    val timeout = FRAMES_TO_CAPTURE * 1000L + 2000L
                    completionLock.wait(timeout)
                }
            }

            // Cleanup
            provider.unbindAll()

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning $source camera", e)
        }

        result
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val mediaImage = imageProxy.image ?: run {
                Log.e(TAG, "imageProxy.image is null")
                return null
            }

            val width = mediaImage.width
            val height = mediaImage.height
            Log.e(TAG, "Converting image: ${width}x${height}, format: ${mediaImage.format}")

            // Get planes
            val yPlane = mediaImage.planes[0]
            val uPlane = mediaImage.planes[1]
            val vPlane = mediaImage.planes[2]

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            // Get strides
            val yRowStride = yPlane.rowStride
            val uvRowStride = uPlane.rowStride
            val uvPixelStride = uPlane.pixelStride

            Log.e(TAG, "Y rowStride: $yRowStride, UV rowStride: $uvRowStride, UV pixelStride: $uvPixelStride")

            // Allocate NV21 buffer: Y + interleaved VU
            val nv21 = ByteArray(width * height * 3 / 2)

            // Copy Y plane row by row (handling row stride)
            var pos = 0
            for (row in 0 until height) {
                yBuffer.position(row * yRowStride)
                yBuffer.get(nv21, pos, width)
                pos += width
            }

            // Copy UV planes - interleave V and U for NV21
            val uvHeight = height / 2
            val uvWidth = width / 2

            for (row in 0 until uvHeight) {
                for (col in 0 until uvWidth) {
                    val uvIndex = row * uvRowStride + col * uvPixelStride

                    // NV21 format: VUVUVU...
                    vBuffer.position(uvIndex)
                    nv21[pos++] = vBuffer.get()

                    uBuffer.position(uvIndex)
                    nv21[pos++] = uBuffer.get()
                }
            }

            val yuvImage = android.graphics.YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                width,
                height,
                null
            )

            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, width, height),
                90,
                out
            )

            val imageBytes = out.toByteArray()
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from JPEG bytes")
                return null
            }

            Log.e(TAG, "Bitmap created: ${bitmap.width}x${bitmap.height}")

            // Rotate if needed
            val rotation = imageProxy.imageInfo.rotationDegrees
            if (rotation != 0) {
                Log.e(TAG, "Rotating bitmap by $rotation degrees")
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image: ${e.message}", e)
            null
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider =
        withContext(Dispatchers.Main) {
            val future = ProcessCameraProvider.getInstance(context)
            future.get()
        }

    fun stopScan() {
        Log.d(TAG, "Stopping scan")
        scanJob?.cancel()
        scanJob = null

        try {
            cameraProvider?.unbindAll()
            lifecycleOwner.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan", e)
        }

        isScanning = false
    }

    fun release() {
        stopScan()
        cameraExecutor.shutdown()
        personDetector.close()
    }
}
