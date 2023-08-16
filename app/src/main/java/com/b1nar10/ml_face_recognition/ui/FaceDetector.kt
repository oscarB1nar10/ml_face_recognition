package com.b1nar10.ml_face_recognition.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.b1nar10.ml_face_recognition.ui.utils.mapToViewCoordinates
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import java.io.ByteArrayOutputStream

class FaceDetector(
    private val context: Context,
    private val onFacesDetected: (faces: Array<Rect>, cameraSize: Size, rotationDegrees: Int) -> Unit,
    private val onFaceCropped: (face: Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient()
    private lateinit var cameraSize: Size
    private var isAnalysisActive = true

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
                    cameraSize = Size(image.width, image.height)
                    onFacesDetected(faceRects, cameraSize, imageProxy.imageInfo.rotationDegrees)

                    // Crop the face images and pass them to the callback
                    faces.forEach { face ->
                        try {
                            val faceBitmap =
                                cropFaceBitmap(mediaImage, imageProxy, face.boundingBox)
                            faceBitmap?.let { onFaceCropped(it) }
                        } catch (e: Exception) {
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

    @androidx.camera.core.ExperimentalGetImage
    private fun cropFaceBitmap(image: Image, imageProxy: ImageProxy, boundingBox: Rect): Bitmap? {
        val bitmap = yuv420ToBitmap(image)
        val activity = context as MainActivity
        val viewSize =
            Size(activity.viewBinding.overlayView.width, activity.viewBinding.overlayView.height)

        val boundingBoxViewCoordinates = boundingBox.mapToViewCoordinates(
            cameraSize = cameraSize,
            viewSize = viewSize,
            faceRect = boundingBox,
            rotationDegrees = imageProxy.imageInfo.rotationDegrees
        )

        return bitmap?.let {
            Bitmap.createBitmap(
                it,
                boundingBox.left,
                boundingBox.top,
                boundingBox.width(),
                boundingBox.height()
            )
        }
    }

    private fun yuv420ToBitmap(image: Image): Bitmap? {
        if (image.format != ImageFormat.YUV_420_888) {
            throw IllegalArgumentException("Image must be in YUV_420_888 format")
        }

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U and V are swapped in NV21 format
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
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