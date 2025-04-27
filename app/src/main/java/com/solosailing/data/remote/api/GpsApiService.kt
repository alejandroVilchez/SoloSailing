package com.solosailing.data.remote.api

import com.solosailing.data.remote.dto.GpsPointDto
import retrofit2.http.GET

interface GpsApiService{
    @GET("api/gps/regattas")
    suspend fun getRegattaPoints(): List<GpsPointDto>
}

