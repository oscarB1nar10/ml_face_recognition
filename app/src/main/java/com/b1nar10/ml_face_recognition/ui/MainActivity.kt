package com.b1nar10.ml_face_recognition.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
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
import com.b1nar10.ml_face_recognition.R
import com.b1nar10.ml_face_recognition.databinding.ActivityMainBinding
import com.b1nar10.ml_face_recognition.ui.utils.mapToViewCoordinates
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: FaceDetectorViewModel by viewModels()

    lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetector: FaceDetector

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

        cameraExecutor = Executors.newSingleThreadExecutor()
        faceDetector = FaceDetector(
            context = this,
            onFacesDetected = ::onFacesDetected,
            onFaceCropped = ::onFaceCropped
        )

        observeUiState()
    }

    private fun observeUiState() {
        viewModel.recognitionState.onEach { state ->
            when (state) {
                is RecognitionState.Loading -> {
                    // Show loading indicator
                }

                is RecognitionState.Recognized -> {
                    // Display the recognized name
                    showPersonName(state.name)
                }

                is RecognitionState.Unknown -> {
                    // Prompt the user to enter a name
                    faceDetector.stop()
                    showNameInputDialog(state.faceBitmap)
                }

                is RecognitionState.Error -> {
                    // Display an error message
                }
            }
        }.launchIn(lifecycleScope)
    }

    // Show person's name when be recognized
    private fun showPersonName(name: String) {
        Toast.makeText(this, "Hello $name", Toast.LENGTH_LONG).show()

        // Wait for 1 second before restarting face analysis
        Handler(Looper.getMainLooper()).postDelayed({
            faceDetector.start()
        }, 1000)
    }

    private fun showNameInputDialog(faceBitmap: Bitmap) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_name, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val faceImageView = dialogView.findViewById<ImageView>(R.id.faceImageView)

        faceImageView.setImageBitmap(faceBitmap)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Enter Name")
            .setPositiveButton("Save") { _, _ ->
                val name = nameEditText.text.toString()
                if (name.isNotEmpty()) {
                    viewModel.saveNewFace(name, faceBitmap)
                } else {
                    Toast.makeText(this, "Name cannot be empty!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                faceDetector.start()
            }
            .show()
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

    private fun onFacesDetected(faces: Array<Rect>, cameraSize: Size, rotationDegrees: Int) {
        val viewSize = Size(viewBinding.overlayView.width, viewBinding.overlayView.height)

        val transformedFaces =
            faces.map { it.mapToViewCoordinates(cameraSize, viewSize, it, rotationDegrees) }
                .toTypedArray()
        runOnUiThread {
            viewBinding.overlayView.updateFaces(transformedFaces)
        }
    }

    private fun onFaceCropped(face: Bitmap) {
        viewModel.analyzeFaceImage(face)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "ml-face-recognition"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(Manifest.permission.CAMERA)
                .apply {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }.toTypedArray()
    }
}