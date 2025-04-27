package com.solosailing.data.repository

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

interface TrackingService{
    @POST("/api/regattas")
    suspend fun createRegatta(@Body body: CreateRegattaRequest): CreateRegattaResponse
}

data class CreateRegattaRequest(val name: String)
data class CreateRegattaResponse(
    @SerializedName("_id") val regattaId: String,
    val owner: String,
    val name: String,
    val createdAt: String
)