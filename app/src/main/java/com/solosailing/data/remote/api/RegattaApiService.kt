package com.solosailing.data.remote.api

import com.solosailing.data.remote.dto.GpsPointDto
import com.solosailing.data.remote.dto.RegattaDto
import retrofit2.http.GET
import retrofit2.http.Path

interface RegattaApiService {
    @GET("api/regattas")
    suspend fun listRegattas(): List<RegattaDto>

    @GET("api/regattas/{regattaId}/points")
    suspend fun getRegattaPoints(@Path("regattaId") id: String): List<GpsPointDto>
}
