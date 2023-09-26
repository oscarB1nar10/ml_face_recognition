package com.b1nar10.ml_face_recognition.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.b1nar10.ml_face_recognition.data.model.PersonModel
import com.b1nar10.ml_face_recognition.databinding.ActivityMainBinding
import com.b1nar10.ml_face_recognition.ui.utils.FaceContourGraphic
import com.b1nar10.ml_face_recognition.ui.utils.saveBitmapToTempFile
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Inject the TextToSpeech instance using Dagger Hilt
    @Inject
    lateinit var textToSpeech: TextToSpeech

    @Inject
    lateinit var faceDetector: FaceDetector

    @Inject
    lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetection: FaceDetection

    private val viewModel: FaceDetectorViewModel by viewModels()
    lateinit var viewBinding: ActivityMainBinding

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        checkPermissionsStatus()
        // Observe changes in the UI state
        observeUiState()
        // Configure UI elements and utilities
        configureUi()
    }

    // Check and request necessary permissions
    private fun checkPermissionsStatus() {
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
    }

    private fun configureUi() {
        // Configure face detection utility
        faceDetection = FaceDetection(
            activity = this,
            faceDetector = faceDetector,
            onFacesDetected = ::onFacesDetected,
            onFaceCropped = ::onFaceCropped,
        )
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                handleUiState(uiState)
            }
        }
    }

    private fun handleUiState(uiState: FaceRecognitionUiState) {
        when (uiState.recognitionState) {

            is RecognitionState.Idle -> {
                startFaceDetector()
            }

            is RecognitionState.Recognized -> {
                // Handle recognized state
                val name = uiState.recognitionState.name
                showPersonName(name)
            }

            is RecognitionState.Unknown -> {
                // Handle unknown state
                uiState.recognitionState.faceBitmap?.let { showNameInputDialog(it) }
            }

            is RecognitionState.Error -> {
                // Handle error state
                val errorMessage = uiState.recognitionState.message
                showErrorDialog(errorMessage)
            }

            else -> {}
        }
    }

    // Start face detection after delay
    private fun startFaceDetector(delayToStartRecognition: Long = 1000) {
        // Wait for 1 second before restarting face analysis
        Handler(Looper.getMainLooper()).postDelayed({
            println("startFaceDetector")
            faceDetection.start()
        }, delayToStartRecognition)
    }

    // Display recognized person's name and speak it
    private fun showPersonName(name: String) {
        showToast("Hello $name")
        //Toast.makeText(this, "Hello $name", Toast.LENGTH_LONG).show()
        speakText("Hello $name")
        startFaceDetector()
    }

    // Show a dialog to input details for unrecognized faces
    private fun showNameInputDialog(faceBitmap: Bitmap) {
        val dialog = InputDetailsDialogFragment()
        val bitmapPath = saveBitmapToTempFile(faceBitmap, this)
        val args = Bundle()
        args.putString("bitmap_path", bitmapPath)
        dialog.arguments = args

        dialog.setListener(object : InputDetailsDialogFragment.InputDetailsListener {
            override fun onDetailsEntered(id: String, name: String) {
                viewModel.saveNewFace(
                    PersonModel(
                        id = id,
                        personName = name,
                        bitMapImage = faceBitmap
                    )
                )
            }

            override fun cancel() {
                viewModel.onResetRecognitionState()
            }
        })

        dialog.show(supportFragmentManager, "InputDetailsDialogFragment")
    }

    // Display an error dialog
    private fun showErrorDialog(errorMessage: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(errorMessage)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Start the camera and binds its lifecycle to the activity
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, faceDetection)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Handle detected faces and draw overlays on them
    private fun onFacesDetected(faces: List<Face>) = with(viewBinding.graphicOverlay) {
        // Task completed successfully
        if (faces.isEmpty()) {
            showToast("No face found")
            return
        }

        this.clear()
        for (i in faces.indices) {
            val face = faces[i]
            val faceGraphic = FaceContourGraphic(this)
            this.add(faceGraphic)
            faceGraphic.updateFace(face)
        }
    }

    // Handle cropped face images
    private fun onFaceCropped(face: Bitmap) {
        faceDetection.stop()
        viewModel.analyzeFaceImage(face)
    }

    private fun speakText(text: String) {
        val params = Bundle()
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "id")
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "ml-face-recognition"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(Manifest.permission.CAMERA)
                .apply {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }.toTypedArray()
    }
}