package com.example.privaro.data.model

import java.time.Instant

data class UsageLog(
    val id: String = "",
    val timestamp: Instant = Instant.now(),
    val eventType: String = "",          // "sensitive_content_detected", "scan_performed", etc.
    val contentType: String? = null,     // "PASSWORD", "OTP", "CARD_NUMBER", etc.
    val action: String = "",             // "continued", "cancelled", "checked_surroundings"
    val peopleDetected: Int? = null,
    val packageName: String? = null      // App where detected
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "timestamp" to timestamp.toString(),
        "eventType" to eventType,
        "contentType" to contentType,
        "action" to action,
        "peopleDetected" to peopleDetected,
        "packageName" to packageName
    )

    companion object {
        const val EVENT_SENSITIVE_DETECTED = "sensitive_content_detected"
        const val EVENT_SCAN_PERFORMED = "scan_performed"
        const val EVENT_SIGN_IN = "sign_in"
        const val EVENT_SIGN_UP = "sign_up"

        const val ACTION_CONTINUED = "continued"
        const val ACTION_CANCELLED = "cancelled"
        const val ACTION_CHECKED_SURROUNDINGS = "checked_surroundings"
    }
}
