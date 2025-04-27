package com.solosailing.di

import com.google.gson.GsonBuilder
import com.solosailing.auth.AuthInterceptor
import com.solosailing.auth.AuthTokenStore
import com.solosailing.data.remote.api.AuthApiService
import com.solosailing.data.remote.api.GpsApiService
import com.solosailing.data.remote.api.ObstacleApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://backend-production-f962.up.railway.app/"

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStore: AuthTokenStore): AuthInterceptor =
        AuthInterceptor(tokenStore)


    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor, loggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideObstacleApiService(retrofit: Retrofit): ObstacleApiService {
        return retrofit.create(ObstacleApiService::class.java)
    }

     @Provides
     @Singleton
     fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
            retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideGpsApiService(retrofit: Retrofit): GpsApiService =
            retrofit.create(GpsApiService::class.java)
}
