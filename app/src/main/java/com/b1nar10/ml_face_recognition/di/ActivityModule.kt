package com.b1nar10.ml_face_recognition.di

import android.app.Application
import android.speech.tts.TextToSpeech
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Module
@InstallIn(ActivityComponent::class)
class ActivityModule {

    @Provides
    fun provideTextToSpeech(context: Application): TextToSpeech {
        lateinit var textToSpeech: TextToSpeech
        textToSpeech = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech.language = Locale.US
            }
        }

        return  textToSpeech
    }

    @Provides
    fun provideExecutorService(): ExecutorService {
        return Executors.newSingleThreadExecutor()
    }
}