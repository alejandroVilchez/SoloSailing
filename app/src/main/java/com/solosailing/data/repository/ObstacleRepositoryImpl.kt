package com.solosailing.data.repository

import android.util.Log
import com.solosailing.data.remote.api.ObstacleApiService
import com.solosailing.data.remote.dto.CreateObstacleRequest
import com.solosailing.data.remote.dto.ObstacleDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Gestionado por Hilt
class ObstacleRepositoryImpl @Inject constructor(
    private val api: ObstacleApiService // Inyecta el servicio API
) : ObstacleRepository {

    // Emite la lista de obstáculos. Podría añadir caché aquí si fuera necesario.
    override fun getObstaclesStream(): Flow<Result<List<ObstacleDto>>> = flow {
        runCatching { api.getObstacles() }
            .onSuccess { emit(Result.success(it)) }
            .onFailure { emit(Result.failure(it)) }
    }


    override suspend fun createObstacle(obstacle: ObstacleDto): Result<ObstacleDto> =
        runCatching {
            val req = CreateObstacleRequest(
                latitude = obstacle.latitude,
                longitude = obstacle.longitude,
                type = obstacle.type,
                name = obstacle.name
            )
            api.createObstacle(req)
        }

    override suspend fun deleteObstacle(obstacleId: String): Result<Unit> =
        runCatching {
            api.deleteObstacle(obstacleId)
            Unit
        }
}