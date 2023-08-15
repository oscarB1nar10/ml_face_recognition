package com.b1nar10.ml_face_recognition.data.local_datasource

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PersonaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: PersonaEntity)

    @Query("SELECT * FROM persona")
    suspend fun getAllPersonas(): List<PersonaEntity>
}