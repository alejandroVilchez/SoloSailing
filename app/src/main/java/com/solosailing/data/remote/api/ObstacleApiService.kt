package com.solosailing.data.remote.api

import com.solosailing.data.remote.dto.CreateObstacleRequest
import com.solosailing.data.remote.dto.ObstacleDto
import retrofit2.Response
import retrofit2.http.*

interface ObstacleApiService {

    @GET("/api/obstacles")
    suspend fun getObstacles(): List<ObstacleDto>

    @POST("/api/obstacles")
    suspend fun createObstacle(@Body obstacleData: CreateObstacleRequest): ObstacleDto

    @DELETE("/api/obstacles/{id}")
    suspend fun deleteObstacle(@Path("id") obstacleId: String): Response<Unit>
}