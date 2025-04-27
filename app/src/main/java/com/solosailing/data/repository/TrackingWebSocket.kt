package com.solosailing.data.repository

import com.solosailing.auth.AuthTokenStore
import jakarta.inject.Named
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


class TrackingWebSocket(
    private val client: OkHttpClient,
    @Named("trackingWsUrl") private val url: String,
    private val tokenStore: AuthTokenStore
) {
    private var ws: WebSocket? = null
    fun connect(regattaId: String, listener: WebSocketListener) {
        val token = tokenStore.getToken()
        val req = Request.Builder()
            .url("$url?regattaId=$regattaId")
            .also { if (token != null) it.addHeader("Sec-WebSocket-Protocol", token) }
        ws = client.newWebSocket(req.build(), listener)
    }
    fun sendUpdate(update: TrackingUpdate) {
        ws?.send(Json.encodeToString(TrackingUpdate.serializer(), update))
    }
    fun close() {
        ws?.close(1000, "finished")
    }
}
@Serializable data class TrackingUpdate(
    val regattaId: String,
    val latitude: Double,
    val longitude: Double,
    val yaw: Float,
    val timestamp: Long
)