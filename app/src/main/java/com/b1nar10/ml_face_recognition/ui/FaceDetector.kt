package com.b1nar10.ml_face_recognition.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.media.Image
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection

class FaceDetector(
    private val onFacesDetected: (faces: Array<Rect>, cameraSize: Size, rotationDegrees: Int) -> Unit,
    private val onFaceCropped: (face: Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient()
    var isAnalysisActive = true

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (!isAnalysisActive) {
            imageProxy.close() // Close the image if analysis is not active
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    // Handle the detected faces here
                    val faceRects = faces.map { it.boundingBox }.toTypedArray()
                    val cameraSize = Size(image.width, image.height)
                    onFacesDetected(faceRects, cameraSize, imageProxy.imageInfo.rotationDegrees)

                    // Crop the face images and pass them to the callback
                    faces.forEach { face ->
                        try {
                            val faceBitmap = cropFaceBitmap(mediaImage, face.boundingBox)
                            faceBitmap?.let { onFaceCropped(it) }
                        }catch (e: Exception) {
                            Log.e(TAG, "Error: $e")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Handle any errors here
                }
                .addOnCompleteListener {
                    // When done, close the image
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun cropFaceBitmap(image: Image, boundingBox: Rect): Bitmap? {
        val bitmap = imageToBitmap(image)
        return Bitmap.createBitmap(bitmap, boundingBox.left, boundingBox.top, boundingBox.width(), boundingBox.height())
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    fun start() {
        isAnalysisActive = true
    }

    fun stop() {
        isAnalysisActive = false
    }

    companion object {
        const val TAG = "FaceDetector"
    }
}