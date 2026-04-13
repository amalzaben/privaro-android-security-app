package com.example.privaro.data.api

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var accessToken: String?
        get() = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
        set(value) {
            sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, value).apply()
        }

    var refreshToken: String?
        get() = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
        set(value) {
            sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, value).apply()
        }

    var userId: Long?
        get() {
            val id = sharedPreferences.getLong(KEY_USER_ID, -1L)
            return if (id == -1L) null else id
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit().putLong(KEY_USER_ID, value).apply()
            } else {
                sharedPreferences.edit().remove(KEY_USER_ID).apply()
            }
        }

    var userEmail: String?
        get() = sharedPreferences.getString(KEY_USER_EMAIL, null)
        set(value) {
            sharedPreferences.edit().putString(KEY_USER_EMAIL, value).apply()
        }

    var displayName: String?
        get() = sharedPreferences.getString(KEY_DISPLAY_NAME, null)
        set(value) {
            sharedPreferences.edit().putString(KEY_DISPLAY_NAME, value).apply()
        }

    var authProvider: String?
        get() = sharedPreferences.getString(KEY_AUTH_PROVIDER, null)
        set(value) {
            sharedPreferences.edit().putString(KEY_AUTH_PROVIDER, value).apply()
        }

    val isLoggedIn: Boolean
        get() = accessToken != null

    fun saveAuthResponse(response: com.example.privaro.data.api.dto.AuthResponse) {
        accessToken = response.accessToken
        refreshToken = response.refreshToken
        userId = response.user.id
        userEmail = response.user.email
        displayName = response.user.displayName
        authProvider = response.user.authProvider
    }

    fun clearTokens() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "privaro_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_AUTH_PROVIDER = "auth_provider"

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
