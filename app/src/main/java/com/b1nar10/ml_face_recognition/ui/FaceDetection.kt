package com.b1nar10.ml_face_recognition.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraCharacteristics
import android.media.Image
import android.util.Log
import android.util.Pair
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.b1nar10.ml_face_recognition.ui.utils.getTargetedWidthHeight
import com.b1nar10.ml_face_recognition.ui.utils.rotateBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream

class FaceDetection(
    private val activity: MainActivity,
    private val onFacesDetected: (faces: List<Face>) -> Unit,
    private val onFaceCropped: (face: Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private var detector: FaceDetector
    private lateinit var cameraSize: Size
    private var isAnalysisActive = true

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()

        detector = FaceDetection.getClient(options)
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (!isAnalysisActive) {
            imageProxy.close()
            return
        }

        val mediaProxy = imageProxy.image
        if (mediaProxy != null) {
            val mediaImage = InputImage.fromMediaImage(
                mediaProxy,
                imageProxy.imageInfo.rotationDegrees
            ).mediaImage
            val rawBitMap = mediaImage?.let { yuv420ToBitmap(it) }
            val rotatedBitmap = rawBitMap?.let { rotateBitmap(it, 90f) }
            val resizedBitmap = rotatedBitmap?.let { resizePreviewImage(it) }

            processImage(resizedBitmap, imageProxy)
        } else {
            imageProxy.close()
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImage(bitmap: Bitmap?, imageProxy: ImageProxy) {
        val image = bitmap?.let { InputImage.fromBitmap(it, 0) }
        if (image != null) {
            activity.viewBinding.graphicOverlay.setCameraInfo(image.width, image.height, CameraCharacteristics.LENS_FACING_BACK)
        }

        if (image != null) {
            detector.process(image)
                .addOnSuccessListener { faces ->
                    handleFaceDetectionSuccess(faces, bitmap, image)
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

    @androidx.camera.core.ExperimentalGetImage
    private fun handleFaceDetectionSuccess(faces: List<Face>, bitmapImage: Bitmap, image: InputImage) {
        if (faces.isNotEmpty()) {
            cameraSize = Size(image.width, image.height)
            onFacesDetected(faces)
        }

        // Consider uncommenting or removing the below code based on the requirements.
        faces.forEach { face ->
            try {
                val faceBitmap = cropFaceBitmap(bitmapImage, face.boundingBox)
                faceBitmap?.let { onFaceCropped(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error: $e")
            }
        }
    }

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

        // Ensure the Image is closed to free up resources.
        image.close()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun resizePreviewImage(previewImage: Bitmap): Bitmap {
        // Get the dimensions of the View

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