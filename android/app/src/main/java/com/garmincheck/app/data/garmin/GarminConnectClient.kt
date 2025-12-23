package com.garmincheck.app.data.garmin

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native Kotlin client for Garmin Connect.
 * Ported from python-garminconnect library.
 *
 * This client authenticates via Garmin SSO and fetches health data directly.
 */
@Singleton
class GarminConnectClient @Inject constructor() {

    companion object {
        private const val TAG = "GarminConnect"

        // Garmin endpoints
        private const val BASE_URL = "https://connect.garmin.com"
        private const val SSO_URL = "https://sso.garmin.com/sso"
        private const val SIGNIN_URL = "$SSO_URL/signin"
        private const val MODERN_URL = "$BASE_URL/modern"
        private const val PROXY_URL = "$MODERN_URL/proxy"

        // API endpoints
        private const val USER_PROFILE_URL = "$PROXY_URL/userprofile-service/socialProfile"
        private const val USER_SETTINGS_URL = "$PROXY_URL/userprofile-service/userprofile/user-settings"
        private const val STATS_URL = "$PROXY_URL/usersummary-service/usersummary/daily"
        private const val HEART_RATE_URL = "$PROXY_URL/wellness-service/wellness/dailyHeartRate"
        private const val SLEEP_URL = "$PROXY_URL/wellness-service/wellness/dailySleepData"
        private const val STRESS_URL = "$PROXY_URL/wellness-service/wellness/dailyStress"
        private const val HRV_URL = "$PROXY_URL/hrv-service/hrv"

        // SSO parameters
        private const val SSO_EMBED_URL = "$SSO_URL/embed"
        private const val SSO_PARAMS = "id=gauth-widget&embedWidget=true&gauthHost=$SSO_URL/sso"
    }

    private val gson: Gson = GsonBuilder().create()

    // Cookie jar to persist session
    private val cookieJar = object : CookieJar {
        private val cookies = mutableListOf<Cookie>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies.removeAll { existing ->
                cookies.any { it.name == existing.name && it.domain == existing.domain }
            }
            this.cookies.addAll(cookies)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookies.filter { it.matches(url) }
        }

        fun clear() {
            cookies.clear()
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var isAuthenticated = false
    private var displayName: String? = null

    /**
     * Authenticate with Garmin Connect using email and password.
     */
    suspend fun authenticate(email: String, password: String): GarminAuthResult {
        return withContext(Dispatchers.IO) {
            try {
                cookieJar.clear()
                isAuthenticated = false

                Log.d(TAG, "Starting authentication for $email")

                // Step 1: Get initial SSO page to get CSRF token and cookies
                val ssoParams = mapOf(
                    "id" to "gauth-widget",
                    "embedWidget" to "true",
                    "gauthHost" to SSO_URL,
                    "service" to MODERN_URL,
                    "source" to SIGNIN_URL,
                    "redirectAfterAccountLoginUrl" to MODERN_URL,
                    "redirectAfterAccountCreationUrl" to MODERN_URL
                )

                val ssoUrl = buildUrl(SSO_EMBED_URL, ssoParams)
                val ssoResponse = executeRequest(Request.Builder().url(ssoUrl).get().build())

                if (!ssoResponse.isSuccessful) {
                    return@withContext GarminAuthResult.Error(
                        "Failed to load SSO page: ${ssoResponse.code}",
                        ssoResponse.code
                    )
                }

                val ssoBody = ssoResponse.body?.string() ?: ""
                ssoResponse.close()

                // Extract CSRF token
                val csrfToken = extractCsrfToken(ssoBody)
                if (csrfToken == null) {
                    Log.e(TAG, "Failed to extract CSRF token")
                    return@withContext GarminAuthResult.Error("Failed to get security token")
                }

                Log.d(TAG, "Got CSRF token")

                // Step 2: Submit login form
                val loginParams = mapOf(
                    "id" to "gauth-widget",
                    "embedWidget" to "true",
                    "gauthHost" to SSO_URL,
                    "service" to MODERN_URL,
                    "source" to SIGNIN_URL,
                    "redirectAfterAccountLoginUrl" to MODERN_URL,
                    "redirectAfterAccountCreationUrl" to MODERN_URL
                )

                val loginUrl = buildUrl(SIGNIN_URL, loginParams)

                val formBody = FormBody.Builder()
                    .add("username", email)
                    .add("password", password)
                    .add("embed", "true")
                    .add("_csrf", csrfToken)
                    .build()

                val loginRequest = Request.Builder()
                    .url(loginUrl)
                    .post(formBody)
                    .header("Origin", "https://sso.garmin.com")
                    .header("Referer", ssoUrl)
                    .header("NK", "NT")
                    .build()

                val loginResponse = executeRequest(loginRequest)
                val loginBody = loginResponse.body?.string() ?: ""
                loginResponse.close()

                // Check for login errors
                if (loginBody.contains("LOCKED") || loginBody.contains("locked")) {
                    return@withContext GarminAuthResult.Error("Account is locked. Please try again later.")
                }

                if (loginBody.contains("INVALID_CREDENTIALS") ||
                    loginBody.contains("credentials are incorrect")) {
                    return@withContext GarminAuthResult.Error("Invalid email or password")
                }

                // Extract service ticket from response
                val ticketMatch = Pattern.compile("ticket=([^\"]+)\"").matcher(loginBody)
                if (!ticketMatch.find()) {
                    Log.e(TAG, "No ticket found in login response")

                    // Check if MFA is required
                    if (loginBody.contains("MFA") || loginBody.contains("verification")) {
                        return@withContext GarminAuthResult.Error(
                            "Two-factor authentication is enabled. Please disable it temporarily."
                        )
                    }

                    return@withContext GarminAuthResult.Error("Login failed. Please check your credentials.")
                }

                val ticket = ticketMatch.group(1)
                Log.d(TAG, "Got service ticket")

                // Step 3: Exchange ticket for session
                val serviceUrl = "$MODERN_URL?ticket=$ticket"
                val serviceRequest = Request.Builder()
                    .url(serviceUrl)
                    .get()
                    .build()

                val serviceResponse = executeRequest(serviceRequest)
                serviceResponse.close()

                if (!serviceResponse.isSuccessful && serviceResponse.code != 302) {
                    return@withContext GarminAuthResult.Error(
                        "Failed to establish session: ${serviceResponse.code}",
                        serviceResponse.code
                    )
                }

                // Step 4: Verify authentication by fetching user profile
                val profileResult = getUserProfile()
                when (profileResult) {
                    is GarminResult.Success -> {
                        isAuthenticated = true
                        displayName = profileResult.data.displayName ?: email
                        Log.d(TAG, "Authentication successful: $displayName")
                        GarminAuthResult.Success(displayName!!)
                    }
                    is GarminResult.Error -> {
                        GarminAuthResult.Error("Authentication verification failed: ${profileResult.message}")
                    }
                }

            } catch (e: IOException) {
                Log.e(TAG, "Network error during authentication", e)
                GarminAuthResult.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during authentication", e)
                GarminAuthResult.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Get user profile
     */
    suspend fun getUserProfile(): GarminResult<GarminUserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(USER_PROFILE_URL)
                    .get()
                    .header("NK", "NT")
                    .build()

                val response = executeRequest(request)
                val body = response.body?.string() ?: ""
                response.close()

                if (response.code == 401 || response.code == 403) {
                    isAuthenticated = false
                    return@withContext GarminResult.Error("Not authenticated")
                }

                if (!response.isSuccessful) {
                    return@withContext GarminResult.Error("Failed to get profile: ${response.code}")
                }

                val profile = gson.fromJson(body, GarminUserProfile::class.java)
                GarminResult.Success(profile)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user profile", e)
                GarminResult.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Get daily statistics
     */
    suspend fun getDailyStats(date: LocalDate = LocalDate.now()): GarminResult<GarminDailyStats> {
        return withContext(Dispatchers.IO) {
            try {
                val dateStr = date.format(DateTimeFormatter.ISO_DATE)
                val url = "$STATS_URL/$displayName?calendarDate=$dateStr"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("NK", "NT")
                    .build()

                val response = executeRequest(request)
                val body = response.body?.string() ?: ""
                response.close()

                if (response.code == 401 || response.code == 403) {
                    isAuthenticated = false
                    return@withContext GarminResult.Error("Session expired")
                }

                if (!response.isSuccessful) {
                    return@withContext GarminResult.Error("Failed to get stats: ${response.code}")
                }

                val stats = gson.fromJson(body, GarminDailyStats::class.java)
                GarminResult.Success(stats.copy(date = dateStr))

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching daily stats", e)
                GarminResult.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Get heart rate data
     */
    suspend fun getHeartRateData(date: LocalDate = LocalDate.now()): GarminResult<GarminHeartRateData> {
        return withContext(Dispatchers.IO) {
            try {
                val dateStr = date.format(DateTimeFormatter.ISO_DATE)
                val url = "$HEART_RATE_URL/$displayName?date=$dateStr"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("NK", "NT")
                    .build()

                val response = executeRequest(request)
                val body = response.body?.string() ?: ""
                response.close()

                if (!response.isSuccessful) {
                    return@withContext GarminResult.Error("Failed to get heart rate: ${response.code}")
                }

                val data = gson.fromJson(body, GarminHeartRateData::class.java)
                GarminResult.Success(data)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching heart rate data", e)
                GarminResult.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Get sleep data
     */
    suspend fun getSleepData(date: LocalDate = LocalDate.now()): GarminResult<GarminSleepData> {
        return withContext(Dispatchers.IO) {
            try {
                val dateStr = date.format(DateTimeFormatter.ISO_DATE)
                val url = "$SLEEP_URL/$displayName?date=$dateStr"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("NK", "NT")
                    .build()

                val response = executeRequest(request)
                val body = response.body?.string() ?: ""
                response.close()

                if (!response.isSuccessful) {
                    return@withContext GarminResult.Error("Failed to get sleep data: ${response.code}")
                }

                val data = gson.fromJson(body, GarminSleepData::class.java)
                GarminResult.Success(data)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching sleep data", e)
                GarminResult.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Get stress data
     */
    suspend fun getStressData(date: LocalDate = LocalDate.now()): GarminResult<GarminStressData> {
        return withContext(Dispatchers.IO) {
            try {
                val dateStr = date.format(DateTimeFormatter.ISO_DATE)
                val url = "$STRESS_URL/$dateStr"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("NK", "NT")
                    .build()

                val response = executeRequest(request)
                val body = response.body?.string() ?: ""
                response.close()

                if (!response.isSuccessful) {
                    return@withContext GarminResult.Error("Failed to get stress data: ${response.code}")
                }

                val data = gson.fromJson(body, GarminStressData::class.java)
                GarminResult.Success(data)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching stress data", e)
                GarminResult.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Get HRV data
     */
    suspend fun getHrvData(date: LocalDate = LocalDate.now()): GarminResult<GarminHrvData> {
        return withContext(Dispatchers.IO) {
            try {
                val dateStr = date.format(DateTimeFormatter.ISO_DATE)
                val url = "$HRV_URL/$dateStr"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("NK", "NT")
                    .build()

                val response = executeRequest(request)
                val body = response.body?.string() ?: ""
                response.close()

                if (!response.isSuccessful) {
                    // HRV data might not be available for all users/days
                    if (response.code == 404) {
                        return@withContext GarminResult.Success(GarminHrvData())
                    }
                    return@withContext GarminResult.Error("Failed to get HRV data: ${response.code}")
                }

                val data = gson.fromJson(body, GarminHrvData::class.java)
                GarminResult.Success(data)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching HRV data", e)
                GarminResult.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Get all health data for a specific date
     */
    suspend fun getAllHealthData(date: LocalDate = LocalDate.now()): GarminResult<GarminHealthData> {
        if (!isAuthenticated) {
            return GarminResult.Error("Not authenticated")
        }

        return withContext(Dispatchers.IO) {
            try {
                val dateStr = date.format(DateTimeFormatter.ISO_DATE)
                val fetchedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                // Fetch all data
                val stats = when (val r = getDailyStats(date)) {
                    is GarminResult.Success -> r.data
                    is GarminResult.Error -> null
                }

                val sleep = when (val r = getSleepData(date)) {
                    is GarminResult.Success -> r.data
                    is GarminResult.Error -> null
                }

                val heartRate = when (val r = getHeartRateData(date)) {
                    is GarminResult.Success -> r.data
                    is GarminResult.Error -> null
                }

                val stress = when (val r = getStressData(date)) {
                    is GarminResult.Success -> r.data
                    is GarminResult.Error -> null
                }

                val hrv = when (val r = getHrvData(date)) {
                    is GarminResult.Success -> r.data
                    is GarminResult.Error -> null
                }

                val healthData = GarminHealthData(
                    date = dateStr,
                    fetchedAt = fetchedAt,
                    stats = stats,
                    sleep = sleep,
                    heartRate = heartRate,
                    stress = stress,
                    hrv = hrv
                )

                GarminResult.Success(healthData)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching all health data", e)
                GarminResult.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Check if currently authenticated
     */
    fun isLoggedIn(): Boolean = isAuthenticated

    /**
     * Logout and clear session
     */
    fun logout() {
        cookieJar.clear()
        isAuthenticated = false
        displayName = null
    }

    /**
     * Export health data to JSON string
     */
    fun exportToJson(data: GarminHealthData): String {
        return GsonBuilder()
            .setPrettyPrinting()
            .create()
            .toJson(data)
    }

    // Helper methods

    private fun executeRequest(request: Request): Response {
        return httpClient.newCall(request).execute()
    }

    private fun buildUrl(baseUrl: String, params: Map<String, String>): String {
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
        return "$baseUrl?$queryString"
    }

    private fun extractCsrfToken(html: String): String? {
        // Try to find CSRF token in various formats
        val patterns = listOf(
            Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\""),
            Pattern.compile("value=\"([^\"]+)\"\\s+name=\"_csrf\""),
            Pattern.compile("\"_csrf\"\\s*:\\s*\"([^\"]+)\"")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }

        return null
    }
}
