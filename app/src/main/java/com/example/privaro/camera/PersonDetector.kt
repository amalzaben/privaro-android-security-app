package com.example.privaro.camera

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.await

data class PersonDetectionResult(
    val personCount: Int,
    val confidence: Float,
    val cameraSource: CameraSource
)

enum class CameraSource {
    FRONT,
    BACK
}

class PersonDetector {

    companion object {
        private const val TAG = "PersonDetector"
        private const val MIN_CONFIDENCE = 0.3f  // Lowered threshold
    }

    private val objectDetector: ObjectDetector
    private val faceDetector: FaceDetector

    init {
        // Object detector for general detection
        val objectOptions = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        objectDetector = ObjectDetection.getClient(objectOptions)

        // Face detector - more reliable for detecting people
        val faceOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.1f)  // Detect smaller faces (people further away)
            .build()
        faceDetector = FaceDetection.getClient(faceOptions)
    }

    suspend fun detectPeople(bitmap: Bitmap, source: CameraSource): PersonDetectionResult {
        return try {
            Log.e(TAG, "★ Starting detection from $source, bitmap: ${bitmap.width}x${bitmap.height}")

            // Check if bitmap is valid (not all black)
            val samplePixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
            val isBlack = (samplePixel and 0x00FFFFFF) == 0
            Log.e(TAG, "★ Sample pixel at center: ${Integer.toHexString(samplePixel)}, isBlack: $isBlack")

            // Check a few more pixels
            var nonBlackPixels = 0
            for (x in listOf(0, bitmap.width/4, bitmap.width/2, 3*bitmap.width/4, bitmap.width-1)) {
                for (y in listOf(0, bitmap.height/4, bitmap.height/2, 3*bitmap.height/4, bitmap.height-1)) {
                    val pixel = bitmap.getPixel(x.coerceIn(0, bitmap.width-1), y.coerceIn(0, bitmap.height-1))
                    if ((pixel and 0x00FFFFFF) != 0) nonBlackPixels++
                }
            }
            Log.e(TAG, "★ Non-black pixels sampled: $nonBlackPixels / 25")

            val image = InputImage.fromBitmap(bitmap, 0)

            // Run face detection (most reliable for people)
            Log.e(TAG, "★ Running face detection...")
            val faces = faceDetector.process(image).await()
            Log.e(TAG, "★ Faces detected: ${faces.size}")

            // Run object detection as backup
            val detectedObjects = objectDetector.process(image).await()
            Log.e(TAG, "★ Objects detected: ${detectedObjects.size}")

            // For FRONT camera: the user is holding the phone, so we need to filter them out
            // Strategy: Skip the largest face (that's the user), only count additional faces
            // For BACK camera: count all faces
            val imageArea = bitmap.width * bitmap.height
            var personCount = 0
            var totalConfidence = 0f

            if (source == CameraSource.FRONT) {
                // Find the largest face (this is the user)
                var largestFaceArea = 0
                var largestFaceIndex = -1

                for ((index, face) in faces.withIndex()) {
                    val faceArea = face.boundingBox.width() * face.boundingBox.height()
                    val faceAreaPercent = (faceArea.toFloat() / imageArea) * 100
                    Log.e(TAG, "Face $index: bounds=${face.boundingBox}, area=${faceAreaPercent.toInt()}% of image")

                    if (faceArea > largestFaceArea) {
                        largestFaceArea = faceArea
                        largestFaceIndex = index
                    }
                }

                // Count all faces EXCEPT the largest one (the user)
                for ((index, face) in faces.withIndex()) {
                    if (index == largestFaceIndex) {
                        Log.e(TAG, "  Face $index → Ignoring (largest face = USER)")
                    } else {
                        Log.e(TAG, "  Face $index → Counting as OTHER person")
                        personCount++
                        totalConfidence += 0.9f
                    }
                }

                Log.e(TAG, "FRONT camera: ${faces.size} faces total, $personCount other people (excluding user)")
            } else {
                // BACK camera: count all faces
                for ((index, face) in faces.withIndex()) {
                    val faceArea = face.boundingBox.width() * face.boundingBox.height()
                    val faceAreaPercent = (faceArea.toFloat() / imageArea) * 100
                    Log.e(TAG, "Face $index: bounds=${face.boundingBox}, area=${faceAreaPercent.toInt()}% of image")
                    Log.e(TAG, "  → Counting as person (back camera)")
                    personCount++
                    totalConfidence += 0.9f
                }
            }

            // Check objects for additional person detections (BACK camera only)
            // For front camera, we rely only on face detection since object detection
            // would also detect the user's clothing/body
            if (source == CameraSource.BACK) {
                for (obj in detectedObjects) {
                    Log.e(TAG, "Object: trackingId=${obj.trackingId}, bounds=${obj.boundingBox}, labels=${obj.labels.size}")
                    for (label in obj.labels) {
                        Log.e(TAG, "  Label: '${label.text}' confidence: ${label.confidence}")
                        val labelText = label.text.lowercase()
                        // Look for person-related labels
                        if ((labelText.contains("person") ||
                             labelText.contains("human") ||
                             labelText.contains("people") ||
                             labelText.contains("fashion") ||
                             labelText.contains("clothing")) &&
                            label.confidence >= MIN_CONFIDENCE
                        ) {
                            // Only add if we didn't already detect faces
                            if (faces.isEmpty()) {
                                personCount++
                                totalConfidence += label.confidence
                                Log.e(TAG, "★ PERSON via object detection: ${label.text}")
                            }
                            break
                        }
                    }
                }

                // Fallback: If we detected objects but no person labels on BACK camera
                if (personCount == 0 && detectedObjects.isNotEmpty()) {
                    Log.e(TAG, "⚠ No person labels, but ${detectedObjects.size} objects detected - treating as potential person")
                    personCount = 1
                    totalConfidence = 0.5f
                }
            } else {
                Log.e(TAG, "FRONT camera: Skipping object detection (would detect user's body)")
            }

            val avgConfidence = if (personCount > 0) totalConfidence / personCount else 0f

            Log.e(TAG, "★★★ FINAL RESULT from $source: $personCount people, confidence: $avgConfidence ★★★")

            PersonDetectionResult(
                personCount = personCount,
                confidence = avgConfidence,
                cameraSource = source
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error detecting people: ${e.message}", e)
            PersonDetectionResult(0, 0f, source)
        }
    }

    fun close() {
        objectDetector.close()
        faceDetector.close()
    }
}
