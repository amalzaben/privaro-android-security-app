package com.example.privaro.data.model

import java.time.Instant

data class User(
    val id: Long = 0,
    val email: String = "",
    val displayName: String? = null,
    val authProvider: String = "EMAIL",
    val createdAt: Instant = Instant.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "email" to email,
        "displayName" to displayName,
        "authProvider" to authProvider,
        "createdAt" to createdAt.toString()
    )
}
