package com.garmincheck.app.data.api

import com.garmincheck.app.data.model.DashboardData
import com.garmincheck.app.data.model.LoginRequest
import com.garmincheck.app.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit service interface for Garmin Connect API
 */
interface GarminApiService {

    /**
     * Authenticate with Garmin Connect
     */
    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * Logout and invalidate session
     */
    @POST("/api/v1/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>

    /**
     * Get dashboard data for authenticated user
     */
    @GET("/api/v1/health/dashboard")
    suspend fun getDashboardData(
        @Header("Authorization") token: String,
        @Query("target_date") targetDate: String? = null
    ): Response<DashboardData>

    /**
     * Health check endpoint
     */
    @GET("/")
    suspend fun healthCheck(): Response<Map<String, Any>>
}
