package com.example.privaro.data.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Use 10.0.2.2 for Android emulator, or your computer's IP for physical device
    private const val BASE_URL = "http://172.19.23.62:8080/"

    @Volatile
    private var retrofit: Retrofit? = null

    @Volatile
    private var authApi: AuthApi? = null

    @Volatile
    private var logApi: LogApi? = null

    fun getAuthApi(context: Context): AuthApi {
        return authApi ?: synchronized(this) {
            authApi ?: createAuthApi(context).also { authApi = it }
        }
    }

    private fun createAuthApi(context: Context): AuthApi {
        return getRetrofit(context).create(AuthApi::class.java)
    }

    fun getLogApi(context: Context): LogApi {
        return logApi ?: synchronized(this) {
            logApi ?: createLogApi(context).also { logApi = it }
        }
    }

    private fun createLogApi(context: Context): LogApi {
        return getRetrofit(context).create(LogApi::class.java)
    }

    private fun getRetrofit(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: createRetrofit(context).also { retrofit = it }
        }
    }

    private fun createRetrofit(context: Context): Retrofit {
        val tokenManager = TokenManager.getInstance(context)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Helper function to change base URL (for testing or production)
    fun updateBaseUrl(newBaseUrl: String, context: Context) {
        synchronized(this) {
            val tokenManager = TokenManager.getInstance(context)

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenManager))
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(newBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            authApi = retrofit?.create(AuthApi::class.java)
            logApi = retrofit?.create(LogApi::class.java)
        }
    }
}
