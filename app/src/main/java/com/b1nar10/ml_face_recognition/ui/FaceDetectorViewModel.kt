package com.b1nar10.ml_face_recognition.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b1nar10.ml_face_recognition.data.FaceAnalyzerRepository
import com.b1nar10.ml_face_recognition.data.model.PersonModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FaceDetectorViewModel @Inject constructor(
    private val faceAnalyzerRepository: FaceAnalyzerRepository
) : ViewModel() {

    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Loading)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState

    fun analyzeFaceImage(bitMapImage: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val personModel = faceAnalyzerRepository.analyzeFaceImage(bitMapImage)
                _recognitionState.value =
                    if (personModel.personName == "Unknown") RecognitionState.Unknown(personModel.bitMapImage) else RecognitionState.Recognized(
                        personModel.personName
                    )
            } catch (e: Exception) {
                // Handle any exceptions that occurred during recognition
                _recognitionState.value = RecognitionState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun saveNewFace(name: String, faceBitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isSaved = faceAnalyzerRepository.saveNewFace(PersonModel(name, faceBitmap))
                _recognitionState.value =
                    if (isSaved) {
                        RecognitionState.Recognized(name)
                    }else {
                        RecognitionState.Unknown(faceBitmap)
                    }
            } catch (e: Exception) {
                // Handle any exceptions that occurred during recognition
                _recognitionState.value = RecognitionState.Error(e.message ?: "Unknown error")
            }
        }
    }
}