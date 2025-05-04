package com.solosailing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solosailing.data.remote.api.RegattaApiService
import com.solosailing.data.remote.dto.RegattaDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PastRegattasListViewModel @Inject constructor(
    private val api: RegattaApiService
) : ViewModel() {
    private val _regs = MutableStateFlow<List<RegattaDto>>(emptyList())
    val regs: StateFlow<List<RegattaDto>> = _regs

    init {
        viewModelScope.launch {
            runCatching { api.listRegattas() }
                .onSuccess { _regs.value = it }
            // .onFailure {  }
        }
    }
}
