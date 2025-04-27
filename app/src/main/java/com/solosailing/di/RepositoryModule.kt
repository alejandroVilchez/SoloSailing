package com.solosailing.di

import com.solosailing.data.repository.ObstacleRepository
import com.solosailing.data.repository.ObstacleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindObstacleRepository(impl: ObstacleRepositoryImpl): ObstacleRepository
}