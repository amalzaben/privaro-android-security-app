package com.example.privaro.data.log

import android.content.Context
import android.util.Log
import com.example.privaro.data.api.LogApi
import com.example.privaro.data.api.RetrofitClient
import com.example.privaro.data.api.dto.ScanLogRequest
import com.example.privaro.data.api.dto.ScanLogResponse
import com.example.privaro.data.model.UsageLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LogRepository private constructor(context: Context) {

    private val logApi: LogApi = RetrofitClient.getLogApi(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory cache for display performance
    private var cachedLogs: MutableList<UsageLog> = mutableListOf()
    private var lastFetchTime: Long = 0
    private val cacheValidityMs = 30_000L // 30 seconds

    companion object {
        private const val TAG = "LogRepository"

        @Volatile
        private var instance: LogRepository? = null

        fun getInstance(context: Context): LogRepository {
            return instance ?: synchronized(this) {
                instance ?: LogRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Log an event to the backend (fire-and-forget).
     * Also updates the local cache optimistically.
     */
    fun logEvent(
        eventType: String,
        contentType: String? = null,
        action: String,
        peopleDetected: Int? = null,
        packageName: String? = null
    ): Result<String> {
        val timestamp = Instant.now()
        val tempId = "temp_${System.currentTimeMillis()}"

        // Optimistically add to local cache
        val localLog = UsageLog(
            id = tempId,
            timestamp = timestamp,
            eventType = eventType,
            contentType = contentType,
            action = action,
            peopleDetected = peopleDetected,
            packageName = packageName
        )
        synchronized(cachedLogs) {
            cachedLogs.add(0, localLog)
        }

        // Fire-and-forget API call
        scope.launch {
            try {
                val request = ScanLogRequest(
                    eventType = eventType,
                    contentType = contentType,
                    action = action,
                    peopleDetected = peopleDetected,
                    packageName = packageName,
                    createdAt = formatTimestamp(timestamp)
                )

                val response = logApi.createLog(request)
                if (response.isSuccessful) {
                    Log.d(TAG, "Log synced to backend: $eventType - $action")
                    // Update the temp ID with the real ID from backend
                    response.body()?.let { backendLog ->
                        synchronized(cachedLogs) {
                            val index = cachedLogs.indexOfFirst { it.id == tempId }
                            if (index >= 0) {
                                cachedLogs[index] = mapResponseToUsageLog(backendLog)
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Failed to sync log to backend: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing log to backend: ${e.message}")
            }
        }

        Log.d(TAG, "Logged event: $eventType - $action")
        return Result.success(tempId)
    }

    fun logSensitiveContentDetected(
        contentType: String,
        action: String,
        peopleDetected: Int? = null,
        packageName: String? = null
    ): Result<String> {
        return logEvent(
            eventType = UsageLog.EVENT_SENSITIVE_DETECTED,
            contentType = contentType,
            action = action,
            peopleDetected = peopleDetected,
            packageName = packageName
        )
    }

    fun logScanPerformed(
        peopleDetected: Int,
        action: String
    ): Result<String> {
        return logEvent(
            eventType = UsageLog.EVENT_SCAN_PERFORMED,
            action = action,
            peopleDetected = peopleDetected
        )
    }

    fun logSignIn(): Result<String> {
        return logEvent(
            eventType = UsageLog.EVENT_SIGN_IN,
            action = "success"
        )
    }

    fun logSignUp(): Result<String> {
        return logEvent(
            eventType = UsageLog.EVENT_SIGN_UP,
            action = "success"
        )
    }

    /**
     * Get recent logs from cache.
     * Use fetchLogsFromBackend() to refresh the cache first.
     */
    fun getRecentLogs(limit: Int = 50): Result<List<UsageLog>> {
        return try {
            synchronized(cachedLogs) {
                Result.success(cachedLogs.take(limit).toList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting logs: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetch logs from backend and update local cache.
     */
    suspend fun fetchLogsFromBackend(limit: Int = 50, offset: Int = 0): Result<List<UsageLog>> {
        return try {
            val response = logApi.getLogs(limit, offset)
            if (response.isSuccessful) {
                val logs = response.body()?.logs?.map { mapResponseToUsageLog(it) } ?: emptyList()
                synchronized(cachedLogs) {
                    if (offset == 0) {
                        cachedLogs.clear()
                    }
                    cachedLogs.addAll(logs)
                }
                lastFetchTime = System.currentTimeMillis()
                Log.d(TAG, "Fetched ${logs.size} logs from backend")
                Result.success(logs)
            } else {
                Log.w(TAG, "Failed to fetch logs: ${response.code()}")
                Result.failure(Exception("Failed to fetch logs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching logs from backend: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Check if cache needs refresh.
     */
    fun isCacheStale(): Boolean {
        return System.currentTimeMillis() - lastFetchTime > cacheValidityMs
    }

    fun getLastScanLog(): UsageLog? {
        return try {
            synchronized(cachedLogs) {
                cachedLogs.firstOrNull { it.eventType == UsageLog.EVENT_SCAN_PERFORMED }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last scan log: ${e.message}")
            null
        }
    }

    fun getScanLogs(): List<UsageLog> {
        return try {
            synchronized(cachedLogs) {
                cachedLogs.filter { it.eventType == UsageLog.EVENT_SCAN_PERFORMED }.toList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting scan logs: ${e.message}")
            emptyList()
        }
    }

    /**
     * Clear local cache. Note: This does NOT delete logs from the backend.
     */
    fun clearLogs() {
        synchronized(cachedLogs) {
            cachedLogs.clear()
        }
        lastFetchTime = 0
    }

    private fun mapResponseToUsageLog(response: ScanLogResponse): UsageLog {
        return UsageLog(
            id = response.id.toString(),
            timestamp = parseTimestamp(response.createdAt),
            eventType = response.eventType,
            contentType = response.contentType,
            action = response.action,
            peopleDetected = response.peopleDetected,
            packageName = response.packageName
        )
    }

    private fun formatTimestamp(instant: Instant): String {
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    private fun parseTimestamp(timestamp: String): Instant {
        return try {
            LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse timestamp: $timestamp, using now()")
            Instant.now()
        }
    }
}
