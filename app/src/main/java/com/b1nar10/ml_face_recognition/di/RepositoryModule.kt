package com.b1nar10.ml_face_recognition.di

import android.content.Context
import com.b1nar10.ml_face_recognition.data.FaceAnalyzerImpl
import com.b1nar10.ml_face_recognition.data.FaceAnalyzerRepository
import com.b1nar10.ml_face_recognition.data.loadModelFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Provides
    @Singleton
    fun provideTfModel(@ApplicationContext context: Context): MappedByteBuffer {
       return context.assets.loadModelFile("mobilenet_v2.tflite")
    }


    @Provides
    @Singleton
    fun provideInterpreter(mappedByteBuffer: MappedByteBuffer): Interpreter {
        /*val options = Interpreter.Options()
        options.numThreads = 4
        options.useNNAPI = false // Disable NNAPI*/
        return Interpreter(mappedByteBuffer)
    }

    @Provides
    @Singleton
    fun provideFaceAnalyzerRepository(
        faceAnalyzerImpl: FaceAnalyzerImpl
    ): FaceAnalyzerRepository = faceAnalyzerImpl

}