package com.b1nar10.ml_face_recognition.data

import android.graphics.Bitmap
import com.b1nar10.ml_face_recognition.data.model.PersonModel

interface FaceAnalyzerRepository {
    suspend fun analyzeFaceImage(bitmapImage: Bitmap): PersonModel
    suspend fun saveNewFace(personModel: PersonModel): Boolean
}