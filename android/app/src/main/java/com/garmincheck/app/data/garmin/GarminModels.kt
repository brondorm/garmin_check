package com.garmincheck.app.data.garmin

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

/**
 * Garmin Connect authentication result
 */
sealed class GarminAuthResult {
    data class Success(val displayName: String) : GarminAuthResult()
    data class Error(val message: String, val code: Int? = null) : GarminAuthResult()
}

/**
 * Generic result wrapper for Garmin API calls
 */
sealed class GarminResult<out T> {
    data class Success<T>(val data: T) : GarminResult<T>()
    data class Error(val message: String) : GarminResult<Nothing>()
}

/**
 * User profile from Garmin Connect
 */
data class GarminUserProfile(
    val displayName: String?,
    val userName: String?,
    val profileImageUrlLarge: String?,
    val profileImageUrlMedium: String?,
    val profileImageUrlSmall: String?
)

/**
 * Daily statistics summary
 */
data class GarminDailyStats(
    val date: String,
    @SerializedName("totalSteps") val totalSteps: Int = 0,
    @SerializedName("totalDistanceMeters") val totalDistanceMeters: Double = 0.0,
    @SerializedName("activeKilocalories") val activeCalories: Int = 0,
    @SerializedName("totalKilocalories") val totalCalories: Int = 0,
    @SerializedName("restingHeartRate") val restingHeartRate: Int = 0,
    @SerializedName("minHeartRate") val minHeartRate: Int = 0,
    @SerializedName("maxHeartRate") val maxHeartRate: Int = 0,
    @SerializedName("averageStressLevel") val averageStressLevel: Int = 0,
    @SerializedName("floorsAscended") val floorsAscended: Int = 0,
    @SerializedName("floorsDescended") val floorsDescended: Int = 0,
    @SerializedName("bodyBatteryChargedValue") val bodyBatteryHigh: Int = 0,
    @SerializedName("bodyBatteryDrainedValue") val bodyBatteryLow: Int = 0
)

/**
 * Sleep data
 */
data class GarminSleepData(
    @SerializedName("dailySleepDTO") val dailySleep: DailySleepDTO? = null,
    @SerializedName("sleepMovement") val sleepMovement: List<Any>? = null,
    @SerializedName("sleepLevels") val sleepLevels: List<SleepLevel>? = null,
    @SerializedName("averageSpO2Value") val averageSpO2: Double? = null,
    @SerializedName("lowestSpO2Value") val lowestSpO2: Int? = null,
    @SerializedName("averageRespirationValue") val averageRespiration: Double? = null
)

data class DailySleepDTO(
    @SerializedName("sleepStartTimestampLocal") val sleepStart: Long? = null,
    @SerializedName("sleepEndTimestampLocal") val sleepEnd: Long? = null,
    @SerializedName("sleepTimeSeconds") val totalSleepSeconds: Int = 0,
    @SerializedName("deepSleepSeconds") val deepSleepSeconds: Int = 0,
    @SerializedName("lightSleepSeconds") val lightSleepSeconds: Int = 0,
    @SerializedName("remSleepSeconds") val remSleepSeconds: Int = 0,
    @SerializedName("awakeSleepSeconds") val awakeSeconds: Int = 0,
    @SerializedName("sleepScores") val sleepScores: SleepScores? = null
)

data class SleepScores(
    val overall: SleepScoreValue? = null,
    val quality: SleepScoreValue? = null,
    val recovery: SleepScoreValue? = null
)

data class SleepScoreValue(
    val value: Int = 0,
    val qualifierKey: String? = null
)

data class SleepLevel(
    val startGMT: Long? = null,
    val endGMT: Long? = null,
    val activityLevel: String? = null,
    val seconds: Int = 0
)

/**
 * Heart rate data
 */
data class GarminHeartRateData(
    @SerializedName("restingHeartRate") val restingHeartRate: Int = 0,
    @SerializedName("minHeartRate") val minHeartRate: Int = 0,
    @SerializedName("maxHeartRate") val maxHeartRate: Int = 0,
    @SerializedName("lastSevenDaysAvgRestingHeartRate") val weeklyAvgRestingHr: Int = 0,
    @SerializedName("heartRateValues") val heartRateValues: List<List<Long>>? = null
) {
    fun getAverageHeartRate(): Double {
        val validValues = heartRateValues
            ?.mapNotNull { if (it.size > 1 && it[1] > 0) it[1].toDouble() else null }
            ?: emptyList()
        return if (validValues.isNotEmpty()) validValues.average() else 0.0
    }
}

/**
 * Stress data
 */
data class GarminStressData(
    @SerializedName("overallStressLevel") val overallStressLevel: Int = 0,
    @SerializedName("stressDuration") val stressDuration: Int = 0,
    @SerializedName("restStressDuration") val restStressDuration: Int = 0,
    @SerializedName("lowStressDuration") val lowStressDuration: Int = 0,
    @SerializedName("mediumStressDuration") val mediumStressDuration: Int = 0,
    @SerializedName("highStressDuration") val highStressDuration: Int = 0,
    @SerializedName("stressValuesArray") val stressValues: List<List<Int>>? = null
) {
    fun getAverageStress(): Double {
        val validValues = stressValues
            ?.mapNotNull { if (it.size > 1 && it[1] > 0) it[1].toDouble() else null }
            ?: emptyList()
        return if (validValues.isNotEmpty()) validValues.average() else 0.0
    }
}

/**
 * HRV (Heart Rate Variability) data
 */
data class GarminHrvData(
    @SerializedName("hrvSummary") val hrvSummary: HrvSummary? = null,
    @SerializedName("startTimestampGMT") val startTimestamp: Long? = null,
    @SerializedName("endTimestampGMT") val endTimestamp: Long? = null
)

data class HrvSummary(
    @SerializedName("weeklyAvg") val weeklyAvg: Int = 0,
    @SerializedName("lastNight") val lastNight: Int = 0,
    @SerializedName("lastNightAvg") val lastNightAvg: Int = 0,
    @SerializedName("lastNight5MinHigh") val lastNight5MinHigh: Int = 0,
    @SerializedName("baselineLowUpper") val baselineLow: Int = 0,
    @SerializedName("baselineBalancedLow") val baselineBalancedLow: Int = 0,
    @SerializedName("baselineBalancedUpper") val baselineBalancedUpper: Int = 0,
    @SerializedName("status") val status: String = "UNKNOWN",
    @SerializedName("feedbackPhrase") val feedbackPhrase: String = ""
)

/**
 * Combined health data for export
 */
data class GarminHealthData(
    val date: String,
    val fetchedAt: String,
    val stats: GarminDailyStats?,
    val sleep: GarminSleepData?,
    val heartRate: GarminHeartRateData?,
    val stress: GarminStressData?,
    val hrv: GarminHrvData?
) {
    /**
     * Convert to simplified dashboard format
     */
    fun toDashboard(): DashboardHealthData {
        return DashboardHealthData(
            date = date,
            fetchedAt = fetchedAt,
            // HRV
            hrvStatus = hrv?.hrvSummary?.status ?: "UNKNOWN",
            hrvValue = hrv?.hrvSummary?.lastNight ?: 0,
            hrvWeeklyAvg = hrv?.hrvSummary?.weeklyAvg ?: 0,
            hrvFeedback = hrv?.hrvSummary?.feedbackPhrase ?: "",
            // Heart Rate
            restingHr = heartRate?.restingHeartRate ?: stats?.restingHeartRate ?: 0,
            minHr = heartRate?.minHeartRate ?: stats?.minHeartRate ?: 0,
            maxHr = heartRate?.maxHeartRate ?: stats?.maxHeartRate ?: 0,
            avgHr = heartRate?.getAverageHeartRate() ?: 0.0,
            // Sleep
            sleepScore = sleep?.dailySleep?.sleepScores?.overall?.value ?: 0,
            totalSleepHours = (sleep?.dailySleep?.totalSleepSeconds ?: 0) / 3600.0,
            deepSleepMinutes = (sleep?.dailySleep?.deepSleepSeconds ?: 0) / 60,
            remSleepMinutes = (sleep?.dailySleep?.remSleepSeconds ?: 0) / 60,
            lightSleepMinutes = (sleep?.dailySleep?.lightSleepSeconds ?: 0) / 60,
            // Stress
            stressLevel = stress?.overallStressLevel ?: stats?.averageStressLevel ?: 0,
            avgStress = stress?.getAverageStress() ?: 0.0,
            // Body Battery & Steps
            bodyBatteryHigh = stats?.bodyBatteryHigh ?: 0,
            bodyBatteryLow = stats?.bodyBatteryLow ?: 0,
            totalSteps = stats?.totalSteps ?: 0
        )
    }
}

/**
 * Simplified dashboard data
 */
data class DashboardHealthData(
    val date: String,
    val fetchedAt: String,
    // HRV
    val hrvStatus: String,
    val hrvValue: Int,
    val hrvWeeklyAvg: Int,
    val hrvFeedback: String,
    // Heart Rate
    val restingHr: Int,
    val minHr: Int,
    val maxHr: Int,
    val avgHr: Double,
    // Sleep
    val sleepScore: Int,
    val totalSleepHours: Double,
    val deepSleepMinutes: Int,
    val remSleepMinutes: Int,
    val lightSleepMinutes: Int,
    // Stress
    val stressLevel: Int,
    val avgStress: Double,
    // Extras
    val bodyBatteryHigh: Int,
    val bodyBatteryLow: Int,
    val totalSteps: Int
)
