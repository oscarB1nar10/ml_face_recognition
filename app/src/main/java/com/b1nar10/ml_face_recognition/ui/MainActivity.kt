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
import android.util.Pair
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: FaceDetectorViewModel by viewModels()

    lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetector: FaceDetection
    private lateinit var textToSpeech: TextToSpeech

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

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        textToSpeech = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech.language = Locale.US
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        faceDetector = FaceDetection(
            activity = this,
            onFacesDetected = ::onFacesDetected,
            onFaceCropped = ::onFaceCropped
        )

        observeUiState()
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
            is RecognitionState.Loading -> {
                // Handle loading state
            }

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
                // Display the error message or other UI updates
            }
        }
    }

    // Show person's name when be recognized
    private fun showPersonName(name: String) {
        Toast.makeText(this, "Hello $name", Toast.LENGTH_LONG).show()
        speakText("Hello $name estupido")
        startFaceDetector()
    }

    private fun showNameInputDialog(faceBitmap: Bitmap) {
        val dialog = InputDetailsDialogFragment()
        val bitmapPath = saveBitmapToTempFile(faceBitmap, this)
        val args = Bundle()
        args.putString("bitmap_path", bitmapPath)
        dialog.arguments = args

        dialog.setListener(object : InputDetailsDialogFragment.InputDetailsListener {
            override fun onDetailsEntered(id: String, name: String) {
                //viewModel.onResetRecognitionState()
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

    private fun startFaceDetector(delayToStartRecognition: Long = 1000) {
        // Wait for 1 second before restarting face analysis
        Handler(Looper.getMainLooper()).postDelayed({
            println("startFaceDetector")
            faceDetector.start()
        }, delayToStartRecognition)
    }

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
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, faceDetector)
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

    private fun onFacesDetected(faces: List<Face>, cameraSize: Size, rotationDegrees: Int) =
        with(viewBinding.graphicOverlay) {
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

    private fun onFaceCropped(face: Bitmap) {
        faceDetector.stop()
        viewModel.analyzeFaceImage(face)
    }

    fun getTargetedWidthHeight(): Pair<Int, Int> {
        val maxWidthForPortraitMode: Int = viewBinding.viewFinder.width
        val maxHeightForPortraitMode: Int = viewBinding.viewFinder.height
        return Pair(maxWidthForPortraitMode, maxHeightForPortraitMode)
    }

    fun speakText(text: String) {
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