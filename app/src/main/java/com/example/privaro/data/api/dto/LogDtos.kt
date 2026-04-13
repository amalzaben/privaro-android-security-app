package com.example.privaro.data.api.dto

// Request DTO for creating logs
data class ScanLogRequest(
    val eventType: String,
    val contentType: String? = null,
    val action: String,
    val peopleDetected: Int? = null,
    val packageName: String? = null,
    val createdAt: String? = null  // ISO 8601 format
)

// Response DTO for single log
data class ScanLogResponse(
    val id: Long,
    val eventType: String,
    val contentType: String?,
    val action: String,
    val peopleDetected: Int?,
    val packageName: String?,
    val createdAt: String
)

// Response DTO for paginated list of logs
data class ScanLogListResponse(
    val logs: List<ScanLogResponse>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

// Response DTO for batch log creation
data class BatchLogResponse(
    val created: Int,
    val message: String
)
