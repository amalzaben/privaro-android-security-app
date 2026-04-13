package com.example.privaro.data.auth

import android.content.Context
import com.example.privaro.data.api.AuthApi
import com.example.privaro.data.api.RetrofitClient
import com.example.privaro.data.api.TokenManager
import com.example.privaro.data.api.dto.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUser(
    val id: Long,
    val email: String,
    val displayName: String?,
    val authProvider: String
)

class AuthRepository(context: Context) {

    private val tokenManager: TokenManager = TokenManager.getInstance(context)
    private val authApi: AuthApi = RetrofitClient.getAuthApi(context)
    private val gson = Gson()

    private val _authStateFlow = MutableStateFlow<AuthUser?>(null)

    init {
        // Initialize auth state from stored tokens
        if (tokenManager.isLoggedIn) {
            _authStateFlow.value = AuthUser(
                id = tokenManager.userId ?: 0L,
                email = tokenManager.userEmail ?: "",
                displayName = tokenManager.displayName,
                authProvider = tokenManager.authProvider ?: "EMAIL"
            )
        }
    }

    val currentUser: AuthUser?
        get() = _authStateFlow.value

    val isAuthenticated: Boolean
        get() = tokenManager.isLoggedIn

    fun authStateFlow(): Flow<AuthUser?> = _authStateFlow.asStateFlow()

    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        return try {
            val response = authApi.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveAuthResponse(authResponse)

                val user = AuthUser(
                    id = authResponse.user.id,
                    email = authResponse.user.email,
                    displayName = authResponse.user.displayName,
                    authProvider = authResponse.user.authProvider
                )
                _authStateFlow.value = user
                Result.success(user)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBody) ?: "Sign in failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> {
        return try {
            val response = authApi.register(RegisterRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveAuthResponse(authResponse)

                val user = AuthUser(
                    id = authResponse.user.id,
                    email = authResponse.user.email,
                    displayName = authResponse.user.displayName,
                    authProvider = authResponse.user.authProvider
                )
                _authStateFlow.value = user
                Result.success(user)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBody) ?: "Sign up failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<AuthUser> {
        return try {
            val response = authApi.googleSignIn(GoogleAuthRequest(idToken))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveAuthResponse(authResponse)

                val user = AuthUser(
                    id = authResponse.user.id,
                    email = authResponse.user.email,
                    displayName = authResponse.user.displayName,
                    authProvider = authResponse.user.authProvider
                )
                _authStateFlow.value = user
                Result.success(user)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBody) ?: "Google sign in failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            val response = authApi.sendPasswordResetEmail(PasswordResetRequest(email))

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBody) ?: "Failed to send reset email"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun refreshToken(): Result<Unit> {
        val refreshToken = tokenManager.refreshToken ?: return Result.failure(Exception("No refresh token"))

        return try {
            val response = authApi.refreshToken(RefreshTokenRequest(refreshToken))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveAuthResponse(authResponse)

                val user = AuthUser(
                    id = authResponse.user.id,
                    email = authResponse.user.email,
                    displayName = authResponse.user.displayName,
                    authProvider = authResponse.user.authProvider
                )
                _authStateFlow.value = user
                Result.success(Unit)
            } else {
                // Token refresh failed, user needs to sign in again
                signOut()
                Result.failure(Exception("Session expired. Please sign in again."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    fun signOut() {
        tokenManager.clearTokens()
        _authStateFlow.value = null
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrEmpty()) return null
        return try {
            val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
            errorResponse.message
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
