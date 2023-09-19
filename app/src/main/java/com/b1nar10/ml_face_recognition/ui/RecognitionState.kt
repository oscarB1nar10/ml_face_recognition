package com.b1nar10.ml_face_recognition.ui

import android.graphics.Bitmap

sealed class RecognitionState {
    object Loading : RecognitionState()

    object Idle : RecognitionState()
    data class Recognized(
        val name: String = ""
    ) : RecognitionState()

    data class Unknown(
        val faceBitmap: Bitmap? = null
    ) : RecognitionState()

    data class Error(val message: String) : RecognitionState()
}


data class FaceRecognitionUiState(
    val isFaceImageDialogOpen: Boolean = false,
    val recognitionState: RecognitionState = RecognitionState.Loading,
    val lastUpdated: Long = System.currentTimeMillis()
)