package com.solosailing.viewModel

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.solosailing.data.repository.CreateRegattaRequest
import com.solosailing.data.repository.TrackingService
import com.solosailing.data.repository.TrackingUpdate
import com.solosailing.data.repository.TrackingWebSocket
import com.solosailing.sensors.SensorsManager // Import el manager
import dagger.hilt.android.lifecycle.HiltViewModel // Importa anotación Hilt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.WebSocketListener
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val sensorsManager: SensorsManager,
    private val fusedClient: FusedLocationProviderClient,
    private val repo: TrackingService,
    private val wsHelper: TrackingWebSocket
) : ViewModel() {

    private var regattaId: String? = null
    private var sendJob: Job? = null

    private val _trackingActive = MutableStateFlow(false)
    val trackingActive: StateFlow<Boolean> = _trackingActive.asStateFlow()

    private val _trackingTimeLeft = MutableStateFlow(0)
    val trackingTimeLeft: StateFlow<Int> = _trackingTimeLeft.asStateFlow()

    val yaw: StateFlow<Float> = sensorsManager.yaw
    val pitch: StateFlow<Float> = sensorsManager.pitch
    val roll: StateFlow<Float> = sensorsManager.roll
    val isSensorAvailable: StateFlow<Boolean> = sensorsManager.isSensorAvailable


    @SuppressLint("MissingPermission")
    fun startTracking(regattaName: String) = viewModelScope.launch {
        if (_trackingActive.value) return@launch

        runCatching {
            repo.createRegatta(CreateRegattaRequest(regattaName))
        }.onSuccess {
            resp -> regattaId = resp.regattaId
            wsHelper.connect(regattaId!!, object: WebSocketListener(){})
            sensorsManager.startSensorUpdates()
            _trackingActive.value = true
            _trackingTimeLeft.value = 3600
            sendJob?.cancel()
            sendJob = launch{
                while (_trackingActive.value && _trackingTimeLeft.value > 0) {
                    delay(1000L)
                    _trackingTimeLeft.value = _trackingTimeLeft.value - 1
                    if (!_trackingActive.value) break

                    val loc = fusedClient.lastLocation.await() ?: continue
                    wsHelper.sendUpdate(
                        TrackingUpdate(
                            regattaId = regattaId!!,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            yaw = yaw.value,
                            timestamp = System.currentTimeMillis()

                        )
                    )
                }
                stopTracking()
            }
        }.onFailure {
            error -> Log.e("TrackingViewModel", "Error starting tracking: ${error.message}")
        }

    }
    fun stopTracking() {
        sendJob?.cancel()
        sensorsManager.stopSensorUpdates()
        _trackingActive.value = false
        _trackingTimeLeft.value = 0
        wsHelper.close()
    }

    fun calibrateSensorNorth(){
        sensorsManager.calibrateNorth()
    }
}