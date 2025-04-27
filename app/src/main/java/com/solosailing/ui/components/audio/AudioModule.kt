package com.solosailing.ui.components.audio

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module @InstallIn(SingletonComponent::class)
object AudioModule {
    @Singleton @Provides
    fun provideSequencer(@ApplicationContext ctx: Context): AudioSequencer {
        return AudioSequencer(ctx)
    }
}