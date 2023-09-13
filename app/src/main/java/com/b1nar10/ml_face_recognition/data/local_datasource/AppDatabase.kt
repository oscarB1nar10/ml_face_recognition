package com.b1nar10.ml_face_recognition.data.local_datasource

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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

    /*companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "face_recognition_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }*/
}