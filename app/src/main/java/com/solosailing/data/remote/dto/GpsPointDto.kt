package com.solosailing.data.remote.dto

data class GpsPointDto(
    val latitude: Double,
    val longitude: Double,
    val yaw: Float,
    val timestamp: String
)