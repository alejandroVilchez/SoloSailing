// com/solosailing/viewModel/PastRouteViewModel.kt
package com.solosailing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.solosailing.data.remote.api.RegattaApiService
import com.solosailing.data.remote.dto.GpsPointDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


data class PastPoint(
    val latLng: LatLng,
    val yaw: Float
)
private fun GpsPointDto.toPastPoint() =
    PastPoint(
        latLng = LatLng(latitude, longitude),
        yaw   = yaw
    )

@HiltViewModel
class PastRouteViewModel @Inject constructor(
    private val api: RegattaApiService
) : ViewModel() {

    private val _points = MutableStateFlow<List<PastPoint>>(emptyList())
    val points: StateFlow<List<PastPoint>> = _points

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadRoute(regattaId: String) {
        viewModelScope.launch {
            _loading.value = true
            runCatching { api.getRegattaPoints(regattaId) }
                .onSuccess { dtoList ->
                    _points.value = dtoList.map(GpsPointDto::toPastPoint)
                    _error.value = null
                }
                .onFailure { e ->
                    _error.value = e.localizedMessage ?: "Error desconocido"
                }
            _loading.value = false
        }
    }
}
