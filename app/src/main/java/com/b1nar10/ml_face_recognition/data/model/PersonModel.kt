package com.b1nar10.ml_face_recognition.data.model

import android.graphics.Bitmap

data class PersonModel(
    val id: String = "",
    val personName: String = "",
    val bitMapImage: Bitmap
)
