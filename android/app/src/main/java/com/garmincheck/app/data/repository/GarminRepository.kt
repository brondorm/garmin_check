package com.garmincheck.app.data.repository

import com.garmincheck.app.data.api.GarminApiService
import com.garmincheck.app.data.model.ApiResult
import com.garmincheck.app.data.model.DashboardData
import com.garmincheck.app.data.model.LoginRequest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for interacting with Garmin Connect API
 */
@Singleton
class GarminRepository @Inject constructor(
    private val apiService: GarminApiService,
    private val credentialsRepository: CredentialsRepository
) {
    private val gson = Gson()

    /**
     * Login to Garmin Connect
     */
    suspend fun login(email: String, password: String): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.token != null) {
                        // Save credentials and token
                        credentialsRepository.saveCredentials(email, password, body.token)
                        ApiResult.Success(body.token)
                    } else {
                        ApiResult.Error(body?.message ?: "Login failed")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = parseErrorMessage(errorBody) ?: when (response.code()) {
                        401 -> "Invalid email or password"
                        503 -> "Unable to connect to Garmin Connect"
                        else -> "Login failed (${response.code()})"
                    }
                    ApiResult.Error(errorMessage, response.code())
                }
            } catch (e: IOException) {
                ApiResult.Error("Network error. Please check your connection.")
            } catch (e: Exception) {
                ApiResult.Error("An unexpected error occurred: ${e.message}")
            }
        }
    }

    /**
     * Logout and clear session
     */
    suspend fun logout(): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val token = credentialsRepository.getToken()
                if (token != null) {
                    apiService.logout("Bearer $token")
                }
                credentialsRepository.clearCredentials()
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                // Still clear credentials even if logout request fails
                credentialsRepository.clearCredentials()
                ApiResult.Success(Unit)
            }
        }
    }

    /**
     * Get dashboard data
     */
    suspend fun getDashboardData(targetDate: String? = null): ApiResult<DashboardData> {
        return withContext(Dispatchers.IO) {
            try {
                val token = credentialsRepository.getToken()
                    ?: return@withContext ApiResult.Error("Not authenticated", 401)

                val response = apiService.getDashboardData("Bearer $token", targetDate)

                if (response.isSuccessful) {
                    response.body()?.let {
                        ApiResult.Success(it)
                    } ?: ApiResult.Error("Empty response")
                } else {
                    when (response.code()) {
                        401 -> {
                            // Token expired, try to re-login
                            val reLoginResult = reLogin()
                            if (reLoginResult is ApiResult.Success) {
                                // Retry the request
                                getDashboardData(targetDate)
                            } else {
                                ApiResult.Error("Session expired. Please login again.", 401)
                            }
                        }
                        else -> {
                            val errorBody = response.errorBody()?.string()
                            val errorMessage = parseErrorMessage(errorBody)
                                ?: "Failed to fetch data (${response.code()})"
                            ApiResult.Error(errorMessage, response.code())
                        }
                    }
                }
            } catch (e: IOException) {
                ApiResult.Error("Network error. Please check your connection.")
            } catch (e: Exception) {
                ApiResult.Error("An unexpected error occurred: ${e.message}")
            }
        }
    }

    /**
     * Check if server is available
     */
    suspend fun checkServerHealth(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.healthCheck()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Re-login using stored credentials
     */
    private suspend fun reLogin(): ApiResult<String> {
        val email = credentialsRepository.getEmail()
        val password = credentialsRepository.getPassword()

        return if (email != null && password != null) {
            login(email, password)
        } else {
            ApiResult.Error("No stored credentials")
        }
    }

    /**
     * Parse error message from response body
     */
    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrEmpty()) return null

        return try {
            val errorResponse = gson.fromJson(errorBody, Map::class.java)
            errorResponse["detail"]?.toString() ?: errorResponse["error"]?.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean = credentialsRepository.hasToken()
}
