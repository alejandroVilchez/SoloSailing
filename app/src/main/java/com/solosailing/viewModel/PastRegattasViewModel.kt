package com.solosailing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.solosailing.data.remote.api.GpsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PastRegattasViewModel @Inject constructor(
    private val api: GpsApiService
): ViewModel() {

    private val _points = MutableStateFlow<List<LatLng>>(emptyList())
    val points: StateFlow<List<LatLng>> = _points

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init {
        viewModelScope.launch {
            runCatching {
                api.getRegattaPoints()
                    .map { LatLng(it.latitude, it.longitude) }
            }.onSuccess {
                _points.value = it
            }.onFailure {
                // log error, quizá exponer un snackbarStateFlow…
            }
            _loading.value = false
        }
    }
}