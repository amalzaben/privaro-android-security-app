package com.example.privaro.data.api

import com.example.privaro.data.api.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/google")
    suspend fun googleSignIn(@Body request: GoogleAuthRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @POST("api/auth/password-reset")
    suspend fun sendPasswordResetEmail(@Body request: PasswordResetRequest): Response<MessageResponse>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @POST("api/auth/logout")
    suspend fun logout(): Response<MessageResponse>
}
