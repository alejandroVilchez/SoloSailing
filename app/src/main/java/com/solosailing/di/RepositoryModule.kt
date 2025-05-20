package com.solosailing.di

import com.solosailing.data.repository.ObstacleRepository
import com.solosailing.data.repository.ObstacleRepositoryImpl
import com.solosailing.data.remote.api.ObstacleApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideObstacleRepository(
        api: ObstacleApiService
    ): ObstacleRepository = ObstacleRepositoryImpl(api)
}