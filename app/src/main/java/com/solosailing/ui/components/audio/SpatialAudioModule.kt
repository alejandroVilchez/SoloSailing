package com.solosailing.ui.components.audio

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context

@Module
@InstallIn(SingletonComponent::class)
object SpatialAudioModule {
    @Provides @Singleton
    fun provideSpatialEngine(@ApplicationContext ctx: Context): SpatialAudioEngine =
        SpatialAudioEngine(ctx)

    @Provides @Singleton
    fun provideSpatialAudioManager(engine: SpatialAudioEngine, @ApplicationContext ctx: Context): SpatialAudioManager =
        SpatialAudioManager(engine, ctx)
}
