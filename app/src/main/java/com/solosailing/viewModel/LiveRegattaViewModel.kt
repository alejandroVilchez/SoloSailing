package com.solosailing.viewModel

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.solosailing.data.repository.TrackingUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import jakarta.inject.Named
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

@HiltViewModel
class LiveRegattaViewModel @Inject constructor(
    private val client: OkHttpClient,
    @Named("trackingWsUrl") private val wsUrl: String
): ViewModel() {

    private val _route = MutableStateFlow<List<
            LatLng>>(emptyList())
    val route: StateFlow<List<LatLng>> = _route

    private var ws:
            WebSocket? = null

    fun startListening(regattaId: String) {
        if (ws != null) return
        val req = Request.Builder()
            .url("$wsUrl?regattaId=$regattaId").build()

        ws = client.newWebSocket(req, object: WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val upd = Json.decodeFromString(TrackingUpdate.serializer(), text)
                    LatLng(upd.latitude, upd.longitude)
                }.onSuccess { latLng ->
                    _route.update { it + latLng }
                }
            }
        })
    }

    fun stop() {
        ws?.close(1000, "done"); ws = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}