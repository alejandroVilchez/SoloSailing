package com.solosailing.data.remote.api

import com.solosailing.data.remote.dto.GpsPointDto
import com.solosailing.data.remote.dto.RegattaDto
import com.solosailing.data.remote.dto.CreateRegattaRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


interface RegattaApiService {
    @GET("api/regattas")
    suspend fun listRegattas(): List<RegattaDto>

    @GET("api/regattas/{regattaId}/points")
    suspend fun getRegattaPoints(@Path("regattaId") id: String): List<GpsPointDto>

    @POST("api/regattas")
    suspend fun createRegatta(@Body req: CreateRegattaRequest): RegattaDto

    @GET("api/regattas/active")
    suspend fun listActiveRegattas(): List<RegattaDto>

    @POST("api/regattas/{id}/join")
    suspend fun joinRegatta(@Path("id") id: String): Response<Unit>

    @POST("api/regattas/{regattaId}/simulate/start")
    suspend fun startSimulation(@Path("regattaId") id: String): Response<Unit>

    @POST("api/regattas/{regattaId}/simulate/stop")
    suspend fun stopSimulation(@Path("regattaId") id: String): Response<Unit>
}
