// LiveRegattaViewModel.kt
package com.solosailing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.solosailing.auth.AuthTokenStore
import com.solosailing.data.remote.api.RegattaApiService
import com.solosailing.data.repository.TrackingUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Named
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject

data class BoatPoint(val latLng: LatLng, val yaw: Float)

@HiltViewModel
class LiveRegattaViewModel @Inject constructor(
    private val client: OkHttpClient,
    @Named("trackingWsUrl") private val wsUrl: String,
    private val tokenStore: AuthTokenStore,
    private val api: RegattaApiService
) : ViewModel() {

    private val _positions = MutableStateFlow<Map<String, List<BoatPoint>>>(emptyMap())
    val positions: StateFlow<Map<String, List<BoatPoint>>> = _positions.asStateFlow()

    private var ws: WebSocket? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun start(regattaId: String) {
        if (ws != null) return
        val token = tokenStore.getToken() ?: return

        val req = Request.Builder()
            .url("$wsUrl?regattaId=$regattaId")
            .header("Sec-WebSocket-Protocol", token)
            .build()

        ws = client.newWebSocket(req, object: WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val upd = json.decodeFromString<TrackingUpdate>(text)
                val pt = BoatPoint(
                    latLng = LatLng(upd.latitude, upd.longitude),
                    yaw = upd.yaw
                )
                _positions.update { curr ->
                    val list = curr[upd.boatId].orEmpty() + pt
                    curr + (upd.boatId to list)
                }
            }
        })
    }

    fun stop() {
        ws?.close(1000, "bye")
        ws = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
