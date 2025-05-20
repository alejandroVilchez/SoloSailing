// di/TrackingModule.kt
package com.solosailing.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.solosailing.auth.AuthTokenStore
import com.solosailing.data.repository.TrackingWebSocket
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import jakarta.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TrackingModule {

    private const val WS_URL = "wss://backend-production-f962.up.railway.app/ws/tracking"

    @Provides
    @Singleton
    @Named("trackingWsUrl")
    fun provideTrackingWsUrl(): String = WS_URL

    @Provides
    @Singleton
    fun provideFusedLocationClient(
        @ApplicationContext ctx: Context
    ): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(ctx)

    @Provides
    @Singleton
    fun provideTrackingWebSocket(
        client: OkHttpClient,
        @Named("trackingWsUrl") url: String,
        tokenStore: AuthTokenStore
    ): TrackingWebSocket = TrackingWebSocket(client, url, tokenStore)
}