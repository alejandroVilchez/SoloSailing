package com.solosailing.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ObstacleDto(
    // Mapea _id de MongoDB a un campo id en Kotlin
    @SerializedName("_id") val id: String? = null,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val name: String
)