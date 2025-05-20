
package com.solosailing.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegattaDto(
    @SerializedName("_id") val id: String,
    val name: String,
    val isLive: Boolean,
    val createdAt: String
)

data class CreateRegattaRequest(val name: String)
