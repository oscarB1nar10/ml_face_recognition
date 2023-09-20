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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FaceDetectorViewModel @Inject constructor(
    private val faceAnalyzerRepository: FaceAnalyzerRepository
) : ViewModel() {

    // UI state exposed to the UI
    private val _uiState = MutableStateFlow(FaceRecognitionUiState())
    val uiState: StateFlow<FaceRecognitionUiState> = _uiState.asStateFlow()


    fun analyzeFaceImage(bitMapImage: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val personModel = faceAnalyzerRepository.analyzeFaceImage(bitMapImage)

                if (personModel.personName == "Unknown") {
                    _uiState.update {
                        it.copy(
                            recognitionState = RecognitionState.Unknown(
                                faceBitmap = personModel.bitMapImage
                            )
                        )
                    }
                } else if (personModel.personName.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            recognitionState = RecognitionState.Recognized(
                                name = personModel.personName
                            ),
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle any exceptions that occurred during recognition
                _uiState.update {
                    it.copy(
                        recognitionState = RecognitionState.Error(e.message ?: "Unknown error")
                    )
                }
            }
        }
    }

    fun saveNewFace(personModel: PersonModel) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isSaved = faceAnalyzerRepository.saveNewFace(personModel)
                if (isSaved) onResetRecognitionState()
            } catch (e: Exception) {
                // Handle any exceptions that occurred during recognition
                _uiState.update {
                    it.copy(
                        recognitionState = RecognitionState.Error(e.message ?: "Unknown error")
                    )
                }
            }
        }
    }

    fun onResetRecognitionState() {
        _uiState.update {
            it.copy(recognitionState = RecognitionState.Idle)
        }
    }
}