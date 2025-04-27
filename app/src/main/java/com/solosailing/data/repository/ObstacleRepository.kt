
package com.solosailing.data.repository

import com.solosailing.data.remote.dto.ObstacleDto
import kotlinx.coroutines.flow.Flow
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Interfaz
interface ObstacleRepository {

    fun getObstaclesStream(): Flow<Result<List<ObstacleDto>>>

    suspend fun createObstacle(obstacle: ObstacleDto): Result<ObstacleDto>

    suspend fun deleteObstacle(id: String): Result<Unit>
}

