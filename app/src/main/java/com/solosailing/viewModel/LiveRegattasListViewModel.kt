package com.solosailing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solosailing.data.remote.api.RegattaApiService
import com.solosailing.data.remote.dto.RegattaDto
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LiveRegattasListViewModel @Inject constructor(
    private val api: RegattaApiService
) : ViewModel() {
    private val _active = MutableStateFlow<List<RegattaDto>>(emptyList())
    val active: StateFlow<List<RegattaDto>> = _active

    fun load() = viewModelScope.launch {
        _active.value = api.listActiveRegattas()
    }

    fun open(regattaId: String, onOpened: () -> Unit) = viewModelScope.launch {
        runCatching { api.startSimulation(regattaId) }
            .onSuccess { onOpened() }
    }

    suspend fun stopSimulation(regattaId: String) {
        api.stopSimulation(regattaId)
    }
}