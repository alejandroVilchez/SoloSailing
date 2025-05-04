
package com.solosailing.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegattaDto(
    @SerializedName("_id") val id: String,
    val owner: String,
    val name: String,
    val createdAt: String
)
