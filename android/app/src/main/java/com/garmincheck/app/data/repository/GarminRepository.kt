package com.garmincheck.app.data.repository

import com.garmincheck.app.data.garmin.*
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for interacting with Garmin Connect directly
 */
@Singleton
class GarminRepository @Inject constructor(
    private val garminClient: GarminConnectClient,
    private val credentialsRepository: CredentialsRepository
) {

    /**
     * Login to Garmin Connect
     */
    suspend fun login(email: String, password: String): GarminAuthResult {
        val result = garminClient.authenticate(email, password)

        if (result is GarminAuthResult.Success) {
            // Save credentials for future sessions
            credentialsRepository.saveCredentials(email, password)
        }

        return result
    }

    /**
     * Logout and clear session
     */
    fun logout() {
        garminClient.logout()
        credentialsRepository.clearCredentials()
    }

    /**
     * Auto-login using saved credentials
     */
    suspend fun autoLogin(): GarminAuthResult {
        val email = credentialsRepository.getEmail()
        val password = credentialsRepository.getPassword()

        return if (email != null && password != null) {
            login(email, password)
        } else {
            GarminAuthResult.Error("No saved credentials")
        }
    }

    /**
     * Get dashboard data
     */
    suspend fun getDashboardData(date: LocalDate = LocalDate.now()): GarminResult<DashboardHealthData> {
        return when (val result = garminClient.getAllHealthData(date)) {
            is GarminResult.Success -> GarminResult.Success(result.data.toDashboard())
            is GarminResult.Error -> GarminResult.Error(result.message)
        }
    }

    /**
     * Get full health data for export
     */
    suspend fun getFullHealthData(date: LocalDate = LocalDate.now()): GarminResult<GarminHealthData> {
        return garminClient.getAllHealthData(date)
    }

    /**
     * Export health data to JSON string
     */
    fun exportToJson(data: GarminHealthData): String {
        return garminClient.exportToJson(data)
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean = garminClient.isLoggedIn()

    /**
     * Check if user has saved credentials
     */
    fun hasSavedCredentials(): Boolean = credentialsRepository.hasCredentials()
}
