package com.solosailing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solosailing.auth.AuthTokenStore
import com.solosailing.data.remote.api.AuthApiService
import com.solosailing.data.remote.api.LoginRequest
import com.solosailing.data.remote.api.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: AuthApiService,
    private val tokenStore: AuthTokenStore
) : ViewModel() {

    private val _userToken = MutableStateFlow<String?>(tokenStore.getToken())
    val userToken: StateFlow<String?> = _userToken.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            runCatching {
                api.login(LoginRequest(username, password))
            }.onSuccess { resp ->
                tokenStore.saveToken(resp.token)
                _userToken.value = resp.token
            }.onFailure { e ->
                val msg = when (e) {
                    is HttpException -> "Login fallido: ${e.code()}"
                    is IOException  -> "Error de red"
                    else            -> "Error desconocido"
                }
                _error.emit(msg)
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            runCatching {
                api.register(RegisterRequest(username, email, password))
            }.onSuccess {
                // quizá navegar al login; aquí no guardamos token
            }.onFailure { e ->
                val msg = when (e) {
                    is HttpException -> "Registro fallido: ${e.code()}"
                    is IOException  -> "Error de red"
                    else            -> "Error desconocido"
                }
                _error.emit(msg)
            }
        }
    }

    fun logout() {
        tokenStore.clearToken()
        _userToken.value = null
    }
}