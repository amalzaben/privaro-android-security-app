package com.example.privaro.data.api

import com.example.privaro.data.api.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LogApi {

    @POST("api/logs")
    suspend fun createLog(@Body request: ScanLogRequest): Response<ScanLogResponse>

    @POST("api/logs/batch")
    suspend fun createLogs(@Body requests: List<ScanLogRequest>): Response<BatchLogResponse>

    @GET("api/logs")
    suspend fun getLogs(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<ScanLogListResponse>
}
