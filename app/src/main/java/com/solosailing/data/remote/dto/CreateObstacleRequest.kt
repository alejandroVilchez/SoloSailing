package com.solosailing.data.remote.dto

data class CreateObstacleRequest(
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val name: String
)