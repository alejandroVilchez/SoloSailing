package com.solosailing.presentation.map

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.solosailing.data.remote.dto.CreateObstacleRequest
import com.solosailing.data.remote.dto.ObstacleDto
import com.solosailing.data.repository.ObstacleRepository
import com.solosailing.sensors.SensorsManager
import com.solosailing.ui.components.audio.AudioEvent
import com.solosailing.ui.components.audio.AudioManager
import com.solosailing.ui.components.audio.AudioSequencer
import com.solosailing.utils.calculateAzimuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import com.solosailing.R
import com.solosailing.utils.calculateAzimuth
import com.solosailing.utils.calculateDistance

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val application: Application,
    private val obstacleRepository: ObstacleRepository,
    private val audioManager: AudioManager,
    private val sensorsManager: SensorsManager,
    private val sequencer: AudioSequencer

) : ViewModel() {
    companion object {
        private const val BEACH_THRESHOLD_METERS = 300f
    }

    val authority = "${application.packageName}.fileprovider"

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _obstacles = MutableStateFlow<List<ObstacleDto>>(emptyList())
    val obstacles: StateFlow<List<ObstacleDto>> = _obstacles.asStateFlow()
    private val obstacleSampleIds = mutableMapOf<String, Int>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _beachSignalActive = MutableStateFlow(false)
    val beachSignalActive: StateFlow<Boolean> = _beachSignalActive.asStateFlow()

    private val _northSignalActive = MutableStateFlow(false)
    val northSignalActive: StateFlow<Boolean> = _northSignalActive.asStateFlow()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    init {
        loadObstacles()
        audioManager.initialize()


        obstacleSampleIds["Boya"]  = sequencer.loadSample(R.raw.buoy)
        obstacleSampleIds["Bote"]  = sequencer.loadSample(R.raw.boat_1)


        listenToObstacles()
        listenToRollAlert()
        listenToDirectionAlerts()
    }

    fun loadObstacles() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            obstacleRepository.getObstaclesStream()
                .collect { result ->
                    _isLoading.value = false
                    result.onSuccess { fetchedObstacles ->
                        _obstacles.value = fetchedObstacles
                    }.onFailure { e ->
                        _errorMessage.value = "Error al cargar obstáculos: ${e.localizedMessage}"
                    }
                }
        }
    }


    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper() // Correcto
            )
            Log.d("LocationViewModel", "Location updates started.")
        } catch (e: SecurityException) {
            Log.e("LocationViewModel", "Location permission not granted.", e)
            _errorMessage.value = "Location permission not granted."
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->
                _currentLocation.value = location
                Log.d("LocationViewModel", "Location updated: ${location.latitude}, ${location.longitude}")
            }
        }
    }


    private fun listenToObstacles() {
        viewModelScope.launch {
            combine(currentLocation, obstacles, sensorsManager.yaw) { loc, obs, yaw ->
                Triple(loc, obs, yaw)
            }
                .filter { it.first != null }
                .sample(500L)
                .collect { (loc, obsList, yaw) ->
                    val userLoc = loc!!
                    val events = obsList.mapNotNull { obs ->
                        val sid = obstacleSampleIds[obs.type] ?: return@mapNotNull null
                        val d   = calculateDistance(
                            userLoc.latitude, userLoc.longitude,
                            obs.latitude,     obs.longitude
                        )
                        val az  = calculateAzimuth(
                            LatLng(userLoc.latitude, userLoc.longitude),
                            yaw.toFloat(),
                            LatLng(obs.latitude, obs.longitude)
                        )
                        AudioEvent(sid, az, d)
                    }
                    sequencer.playSequence(events)
                }
        }
    }
    private fun listenToRollAlert() {
        viewModelScope.launch {
            sensorsManager.roll
                .collect { roll ->
                    audioManager.scheduleRollAlert(roll)
                }
        }
    }
    private fun listenToDirectionAlerts() {
        viewModelScope.launch {
            combine(
                sensorsManager.yaw,
                currentLocation,
                beachSignalActive,
                northSignalActive
            ) { yaw, loc, beachOn, northOn ->
                Quad(yaw, loc, beachOn, northOn)
            }
                .filter { it.b != null }
                .sample(500L)
                .collect { (yaw, loc, beachOn, northOn) ->
                    val userLoc = loc!!

                    // Norte
                    if (northOn) {
                        audioManager.scheduleNorthSignal(yaw.toFloat())
                    } else {
                        audioManager.stopNorthSignal()
                    }

                    // Playa
                    if (beachOn) {
                        obstacles.value.find { it.type=="Playa" }?.let { beach ->
                            val d  = calculateDistance(
                                userLoc.latitude, userLoc.longitude,
                                beach.latitude,   beach.longitude
                            )
                            val az = calculateAzimuth(
                                LatLng(userLoc.latitude, userLoc.longitude),
                                yaw.toFloat(),
                                LatLng(beach.latitude, beach.longitude)
                            )
                            audioManager.scheduleBeachSignal(
                                beachAzimuth = az,
                                distance     = d,
                                minDistance  = BEACH_THRESHOLD_METERS
                            )
                        }
                    } else {
                        audioManager.stopBeachSignal()
                    }
                }
        }
    }


    fun addObstacle(req: CreateObstacleRequest) {
        viewModelScope.launch {
            obstacleRepository.createObstacle(ObstacleDto(
                id = null,
                latitude  = req.latitude,
                longitude = req.longitude,
                type      = req.type,
                name      = req.name
            )).onSuccess {
                _obstacles.value = _obstacles.value + it
            }.onFailure {
                _errorMessage.value = "Error al añadir obstáculo: ${it.localizedMessage}"
            }
        }
    }

    fun removeObstacle(id: String) {
        viewModelScope.launch {
            obstacleRepository.deleteObstacle(id)
                .onSuccess { _obstacles.value = _obstacles.value.filterNot { it.id == id } }
                .onFailure { _errorMessage.value = "Error al eliminar obstáculo: ${it.localizedMessage}" }
        }
    }

    fun exportObstacles(onResult: (Uri?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = _obstacles.value.map {
                    CreateObstacleRequest(it.latitude, it.longitude, it.type, it.name)
                }
                val json = Gson().toJson(list)
                val file = File(application.cacheDir, "obstacles.json").apply {
                    writeText(json, Charsets.UTF_8)
                }
                val uri = FileProvider.getUriForFile(application, authority, file)
                onResult(uri)
            } catch (e: Exception) {
                Log.e("LocationVM", "export failed", e)
                onResult(null)
            }
        }
    }

    fun importObstacles(json: String, onFinished: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val type = object : TypeToken<List<CreateObstacleRequest>>() {}.type
                val list: List<CreateObstacleRequest> = Gson().fromJson(json, type)
                list.forEach { addObstacle(it) }
                loadObstacles()
                withContext(Dispatchers.Main) { onFinished(true, null) }
            } catch (e: Exception) {
                Log.e("LocationVM", "import failed", e)
                withContext(Dispatchers.Main) { onFinished(false, e.localizedMessage) }
            }
        }
    }



    fun toggleBeachSignal() { _beachSignalActive.value = !_beachSignalActive.value }
    fun toggleNorthSignal() { _northSignalActive.value = !_northSignalActive.value }




    fun clearErrorMessage() { _errorMessage.value = null }

    override fun onCleared() {
        super.onCleared()

        sequencer.release()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        audioManager.release()
    }
}
// Clase auxiliar para combinar 4 flujos
private data class Quad<A,B,C,D>(val a:A, val b:B, val c:C, val d:D)
