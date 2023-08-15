package com.b1nar10.ml_face_recognition.data.local_datasource

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persona")
data class PersonaEntity(
    @PrimaryKey @ColumnInfo(name="name") val name: String,
    @ColumnInfo(name="encoding") val encoding: String
)
