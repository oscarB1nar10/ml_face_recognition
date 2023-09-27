package com.b1nar10.ml_face_recognition.ui

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import android.util.Pair
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.b1nar10.ml_face_recognition.ui.utils.getTargetedWidthHeight
import com.b1nar10.ml_face_recognition.ui.utils.rotateBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector

class FaceDetection(
    private val activity: MainActivity,
    private val faceDetector: FaceDetector,
    private val onFacesDetected: (faces: List<Face>) -> Unit,
    private val onFaceCropped: (face: Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private var isAnalysisActive = true


    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        // If analysis is not active, close the current image
        if (!isAnalysisActive) {
            imageProxy.close()
            return
        }

        // Check if the image format is RGBA_8888
        if (imageProxy.format == PixelFormat.RGBA_8888) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees.toFloat()
            val bitmapBuffer = imageProxy.planes[0].buffer
            // Convert buffer to Bitmap
            val bitmap =
                Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(bitmapBuffer)

            // Rotate and resize the Bitmap
            val rotatedBitmap = bitmap?.let { rotateBitmap(it, rotationDegrees) }
            val resizedBitmap = rotatedBitmap?.let { resizePreviewImage(it) }

            // Process the image for face detection
            processImage(resizedBitmap, imageProxy)
        } else {
            imageProxy.close()
        }
    }

    /**
     * Process the image, detects faces, and triggers callbacks based on detected faces.
     */
    @androidx.camera.core.ExperimentalGetImage
    private fun processImage(bitmap: Bitmap?, imageProxy: ImageProxy) {
        // Convert Bitmap to InputImage fro ML Kit
        val image = bitmap?.let { InputImage.fromBitmap(it, 0) }
        if (image != null) {
            activity.viewBinding.graphicOverlay.setCameraInfo(
                image.width,
                image.height,
                CameraCharacteristics.LENS_FACING_BACK
            )
        }

        // Process the image using ML Kit's Face Detector
        if (image != null) {
            this.faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    handleFaceDetectionSuccess(faces, bitmap)
                }
                .addOnFailureListener { e ->
                    // Handle any errors here. Possibly log them.
                }
                .addOnCompleteListener {
                    // When done, close the image
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    /**
     * Handles the result of successful face detection.
     */
    @androidx.camera.core.ExperimentalGetImage
    private fun handleFaceDetectionSuccess(
        faces: List<Face>,
        bitmapImage: Bitmap
    ) {
        if (faces.isNotEmpty()) {
            onFacesDetected(faces)
        }

        // Process each detected face.
        faces.forEach { face ->
            try {
                val faceBitmap = cropFaceBitmap(bitmapImage, face.boundingBox)
                faceBitmap?.let { onFaceCropped(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error: $e")
            }
        }
    }

    /**
     * Crop the bitmap to focus on the face using the provided bounding box.
     */
    @androidx.camera.core.ExperimentalGetImage
    private fun cropFaceBitmap(bitmapImage: Bitmap, boundingBox: Rect): Bitmap? {
        return bitmapImage.let {
            Bitmap.createBitmap(
                it,
                boundingBox.left,
                boundingBox.top,
                boundingBox.width(),
                boundingBox.height()
            )
        }
    }

    /**
     * Resize the image for the preview while maintaining is aspect ratio.
     */
    private fun resizePreviewImage(previewImage: Bitmap): Bitmap {
        // Get the dimensions of the View
        val targetedSize: Pair<Int, Int> = activity.getTargetedWidthHeight()

        val targetWidth = targetedSize.first
        val maxHeight = targetedSize.second

        // Determine how much to scale down the image
        val scaleFactor: Float = Math.max(
            previewImage.width.toFloat() / targetWidth.toFloat(),
            previewImage.height.toFloat() / maxHeight.toFloat()
        )

        return Bitmap.createScaledBitmap(
            previewImage,
            (previewImage.width / scaleFactor).toInt(),
            (previewImage.height / scaleFactor).toInt(),
            true
        )
    }

    // Start the analysis
    fun start() {
        isAnalysisActive = true
    }

    // Stop the analysis
    fun stop() {
        isAnalysisActive = false
    }

    companion object {
        const val TAG = "FaceDetector"
    }
}