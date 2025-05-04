package com.solosailing.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.solosailing.R
import com.solosailing.data.remote.dto.SimulationData
import com.solosailing.data.remote.dto.IntroBuoyDto
import com.solosailing.data.remote.dto.IntroPositionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class IntroBuoy(val name: String, val latLng: LatLng)
data class IntroPoint(val latLng: LatLng, val yaw: Float)

class RegattaSimulationViewModel(app: Application) : AndroidViewModel(app) {

    private val _buoys = MutableStateFlow<List<IntroBuoy>>(emptyList())
    val buoys: StateFlow<List<IntroBuoy>> = _buoys

    private val _positions = MutableStateFlow<Map<String, List<IntroPoint>>>(emptyMap())
    val positions: StateFlow<Map<String, List<IntroPoint>>> = _positions

    private val jsonParser = Json {
        ignoreUnknownKeys = true
    }

    init {
        viewModelScope.launch {
            val jsonText = getApplication<Application>()
                .resources.openRawResource(R.raw.regatta1)
                .bufferedReader()
                .use { it.readText() }


            val data = jsonParser.decodeFromString<SimulationData>(jsonText)

            _buoys.value = data.buoys.map { IntroBuoy(it.name, LatLng(it.lat, it.lng)) }

            val map = data.positions.mapValues { entry ->
                entry.value.map { p ->
                    IntroPoint(
                        latLng = LatLng(p.a, p.n),
                        yaw    = p.c.toFloat()
                    )
                }
            }
            _positions.value = map
        }
    }
}
