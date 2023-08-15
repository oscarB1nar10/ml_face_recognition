package com.b1nar10.ml_face_recognition.ui

import android.graphics.Bitmap

sealed class RecognitionState {
    object Loading : RecognitionState()
    data class Recognized(val name: String) : RecognitionState()
    data class Unknown(val faceBitmap: Bitmap) : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}
