package com.example.privaro.data.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip adding token for auth endpoints that don't require it
        val path = originalRequest.url.encodedPath
        val noAuthPaths = listOf(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/google",
            "/api/auth/refresh",
            "/api/auth/password-reset"
        )

        if (noAuthPaths.any { path.endsWith(it) }) {
            return chain.proceed(originalRequest)
        }

        // Add authorization header if token exists
        val token = tokenManager.accessToken
        if (token != null) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            return chain.proceed(authenticatedRequest)
        }

        return chain.proceed(originalRequest)
    }
}
