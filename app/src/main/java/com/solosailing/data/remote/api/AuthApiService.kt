package com.solosailing.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String)

data class RegisterRequest(val username: String, val email: String, val password: String)
data class RegisterResponse(val message: String)

interface AuthApiService {
    @POST("api/users/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @POST("api/users/register")
    suspend fun register(@Body req: RegisterRequest): RegisterResponse
}