package com.example.privaro.data.api.dto

import com.google.gson.annotations.SerializedName

// Request DTOs
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class GoogleAuthRequest(
    val idToken: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class PasswordResetRequest(
    val email: String
)

// Response DTOs
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserDto
)

data class UserDto(
    val id: Long,
    val email: String,
    val displayName: String?,
    val authProvider: String
)

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: String
)

data class MessageResponse(
    val message: String
)
