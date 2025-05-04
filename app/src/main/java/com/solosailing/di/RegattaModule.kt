package com.solosailing.di

import com.solosailing.data.remote.api.RegattaApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RegattaModule {
    @Provides @Singleton
    fun provideRegattaApiService(retrofit: Retrofit): RegattaApiService =
        retrofit.create(RegattaApiService::class.java)
}
