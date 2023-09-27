package com.b1nar10.ml_face_recognition.data.local_datasource

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PersonaEntity::class, EmbeddingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personaDao(): PersonaDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun personaWithEmbeddingsDao(): PersonaWithEmbeddingsDao
}