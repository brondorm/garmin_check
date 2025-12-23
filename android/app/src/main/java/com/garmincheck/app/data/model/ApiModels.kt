package com.garmincheck.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Login request model
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Login response model
 */
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?
)

/**
 * Dashboard data model - simplified health metrics
 */
data class DashboardData(
    val date: String,
    @SerializedName("fetched_at") val fetchedAt: String,

    // HRV
    @SerializedName("hrv_status") val hrvStatus: String,
    @SerializedName("hrv_value") val hrvValue: Int,
    @SerializedName("hrv_weekly_avg") val hrvWeeklyAvg: Int,
    @SerializedName("hrv_feedback") val hrvFeedback: String,

    // Heart Rate
    @SerializedName("resting_hr") val restingHr: Int,
    @SerializedName("min_hr") val minHr: Int,
    @SerializedName("max_hr") val maxHr: Int,
    @SerializedName("avg_hr") val avgHr: Double,

    // Sleep
    @SerializedName("sleep_score") val sleepScore: Int,
    @SerializedName("total_sleep_hours") val totalSleepHours: Double,
    @SerializedName("deep_sleep_minutes") val deepSleepMinutes: Int,
    @SerializedName("rem_sleep_minutes") val remSleepMinutes: Int,
    @SerializedName("light_sleep_minutes") val lightSleepMinutes: Int,

    // Stress
    @SerializedName("stress_level") val stressLevel: Int,
    @SerializedName("avg_stress") val avgStress: Double,

    // Body Battery
    @SerializedName("body_battery_high") val bodyBatteryHigh: Int,
    @SerializedName("body_battery_low") val bodyBatteryLow: Int,

    // Steps
    @SerializedName("total_steps") val totalSteps: Int
)

/**
 * Error response model
 */
data class ErrorResponse(
    val error: String? = null,
    val detail: String? = null
)

/**
 * Sealed class representing API result states
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
}
