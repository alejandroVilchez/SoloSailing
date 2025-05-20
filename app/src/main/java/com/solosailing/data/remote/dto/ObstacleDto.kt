package com.solosailing.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ObstacleDto(
    @SerializedName("_id") val id: String? = null,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val name: String
)