package com.solosailing.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.solosailing.R
import com.solosailing.data.remote.dto.SimulationData
import com.solosailing.data.remote.dto.IntroBuoyDto
import com.solosailing.data.remote.dto.IntroPositionDto
import com.solosailing.presentation.map.DirectionMode
import com.solosailing.sensors.SensorsManager
import com.solosailing.ui.components.audio.AudioEvent
import com.solosailing.ui.components.audio.AudioManager
import com.solosailing.ui.components.audio.AudioSequencer
import com.solosailing.ui.components.audio.SpatialAudioManager
import com.solosailing.utils.calculateAzimuth
import com.solosailing.utils.calculateDistance
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.lang.System.console

data class IntroBuoy(val name: String, val latLng: LatLng)
data class IntroPoint(val latLng: LatLng, val yaw: Float)

@HiltViewModel
class RegattaSimulationViewModel @Inject constructor(
    private val application: Application,
    //val sensorsManager: SensorsManager,
    private val audioManager: SpatialAudioManager,
    //private val sequencer: AudioSequencer
) : AndroidViewModel(application) {

    companion object {
        private const val MAX_DISTANCE = 100f
    }

    private val _simIdx = MutableStateFlow(0)
    val simIdx: StateFlow<Int> = _simIdx.asStateFlow()

    private val _buoys      = MutableStateFlow<List<IntroBuoy>>(emptyList())
    val buoys: StateFlow<List<IntroBuoy>> = _buoys

    private val _positions  = MutableStateFlow<Map<String, List<IntroPoint>>>(emptyMap())
    val positions: StateFlow<Map<String, List<IntroPoint>>> = _positions

   //private val boatSampleIds = mutableMapOf<String, Int>()

    private val _mode = MutableStateFlow(DirectionMode.Off)
    val mode: StateFlow<DirectionMode> = _mode.asStateFlow()

    private val _northSignalActive = MutableStateFlow(false)
    val northSignalActive: StateFlow<Boolean> = _northSignalActive.asStateFlow()

    private val _selectedBoatId = MutableStateFlow<String?>(null)
    val selectedBoatId: StateFlow<String?> = _selectedBoatId.asStateFlow()

    private val boatYaw: StateFlow<Float> = combine(
        selectedBoatId.filterNotNull(),
        simIdx
    ) { selBoat: String, idx: Int ->
        _positions.value[selBoat]?.getOrNull(idx)?.yaw
    }.filterNotNull().stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    //private var buoySampleId: Int = 0


    init {
        audioManager.initialize()

        viewModelScope.launch {
            val jsonText = getApplication<Application>()
                .resources.openRawResource(R.raw.regatta1)
                .bufferedReader()
                .use { it.readText() }
            val data = Json { ignoreUnknownKeys = true }
                .decodeFromString<SimulationData>(jsonText)

            _buoys.value = data.buoys.map { IntroBuoy(it.name, LatLng(it.lat, it.lng)) }
            _positions.value = data.positions.mapValues { entry ->
                entry.value.map { p ->
                    IntroPoint(
                        latLng = LatLng(p.a, p.n),
                        yaw    = p.c.toFloat()
                    )
                }
            }
//            val samples = listOf(
//                R.raw.boat_1, R.raw.boat_2, R.raw.boat_3,
//                R.raw.boat_4, R.raw.boat_5, R.raw.boat_6,
//                R.raw.boat_7, R.raw.boat_8, R.raw.boat_9
//            )
//            _positions.value.keys.forEachIndexed { idx, boatId ->
//                boatSampleIds[boatId] =
//                    sequencer.loadSample(samples.getOrElse(idx) { samples.last() })
//            }
//            buoySampleId = sequencer.loadSample(R.raw.buoy)
            _selectedBoatId.value = _positions.value.keys.firstOrNull()
//            listenToSimulationAudio()
        }
        listenToSimulationAudio()
        listenToDirectionAlerts()
    }

    fun selectBoat(id: String) {
        _selectedBoatId.value = id
    }
    fun nextStep() {
        _simIdx.update { it + 1 }
    }
    fun advance(delta: Int = 50) {
        val id = selectedBoatId.value ?: return
        val route = _positions.value[id] ?: return
        val maxIdx = route.lastIndex
        _simIdx.update { old -> (old + delta).coerceAtMost(maxIdx) }
    }

    fun rewind(delta: Int = 50) {
        _simIdx.update { old -> (old - delta).coerceAtLeast(0) }
    }

    fun reset() {
        _simIdx.value = 0
    }

    private fun listenToSimulationAudio() {
        viewModelScope.launch {
            combine(_positions, _buoys, selectedBoatId, simIdx) { routes, buoys, selBoat, idx ->
                Triple(routes, buoys, selBoat) to idx
            }
                .filter { (tuple, idx) ->
                    val (routes, _, selBoat) = tuple
                    selBoat != null && routes[selBoat]?.indices?.contains(idx) == true
                }
                .sample(1000L)
                .collect { (tuple, idx) ->
                    val (routes, buoys, selBoat) = tuple
                    val route = routes[selBoat]!!
                    val vp = route[idx]
                    val origin = vp.latLng
                    val heading = vp.yaw

                    routes.entries
                        .filter { it.key != selBoat }
                        .forEach { (boatId, r) ->
                            r.getOrNull(idx)?.let { pt ->
                                val d  = calculateDistance(
                                    origin.latitude, origin.longitude,
                                    pt.latLng.latitude,    pt.latLng.longitude
                                )
                                if (d <= MAX_DISTANCE) {
                                    val az = calculateAzimuth(origin, heading, pt.latLng)
                                    val index = routes.keys.indexOf(boatId) + 1
                                    audioManager.playBoat(index, az, d)
                                }
                            }
                        }
                    buoys.forEach { buoy ->
                        val d = calculateDistance(
                            origin.latitude, origin.longitude,
                            buoy.latLng.latitude, buoy.latLng.longitude
                        )
                        if (d <= MAX_DISTANCE * 2) {
                            val az = calculateAzimuth(origin, heading, buoy.latLng)
                            audioManager.playBuoy(az, d)
                        }
                    }
                }
        }
    }

    private fun listenToDirectionAlerts() = viewModelScope.launch {
        combine(
            boatYaw,
            mode
        ) { yaw, m -> yaw to m }
            //.filter { it.second == DirectionMode.North }
            .sample(5_000L)
            .collect { (yaw, m) ->
                audioManager.stopNorthSignal()
                when(m){
                    DirectionMode.Beach -> {

                    }
                    DirectionMode.North -> {
                        audioManager.scheduleNorthSignal(yaw, 180f)
                    }
                    DirectionMode.Off -> {
                        audioManager.stopNorthSignal()
                    }
                }
            }
    }

    fun toggleNorthSignal() {
        _mode.value = if (_mode.value == DirectionMode.North) DirectionMode.Off
        else DirectionMode.North
    }


    override fun onCleared() {
        super.onCleared()
        //sequencer.release()
        audioManager.release()
    }
}
