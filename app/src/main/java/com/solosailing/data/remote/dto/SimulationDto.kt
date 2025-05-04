package com.solosailing.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class IntroBuoyDto(
    val id: Long,
    val race_id: Long,
    val name: String,
    val lat: Double,
    val lng: Double
)

@Serializable
data class IntroPositionDto(
    val i: String,
    val a: Double,
    val n: Double,
    val t: Long,
    val s: Double,
    val c: Double
)

@Serializable
data class SimulationData(
    val buoys: List<IntroBuoyDto>,
    val positions: Map<String, List<IntroPositionDto>>,
    val startTmst: Long,
    val endTmst: Long
)
