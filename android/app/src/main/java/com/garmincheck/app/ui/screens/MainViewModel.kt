package com.garmincheck.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garmincheck.app.data.garmin.*
import com.garmincheck.app.data.repository.GarminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val isAutoLogging: Boolean = false,
    val dashboardData: DashboardHealthData? = null,
    val errorMessage: String? = null,
    val exportedFile: File? = null,
    val userName: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: GarminRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Store full data for export
    private var fullHealthData: GarminHealthData? = null

    init {
        checkAndAutoLogin()
    }

    private fun checkAndAutoLogin() {
        if (repository.hasSavedCredentials()) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isAutoLogging = true)

                when (val result = repository.autoLogin()) {
                    is GarminAuthResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoggedIn = true,
                            isAutoLogging = false,
                            userName = result.displayName
                        )
                        refreshData()
                    }
                    is GarminAuthResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isAutoLogging = false,
                            errorMessage = "Auto-login failed: ${result.message}"
                        )
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            when (val result = repository.login(email, password)) {
                is GarminAuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        isLoading = false,
                        userName = result.displayName
                    )
                    refreshData()
                }
                is GarminAuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun logout() {
        repository.logout()
        fullHealthData = null
        _uiState.value = MainUiState(isLoggedIn = false)
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            // Get full data for potential export
            when (val fullResult = repository.getFullHealthData()) {
                is GarminResult.Success -> {
                    fullHealthData = fullResult.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        dashboardData = fullResult.data.toDashboard(),
                        errorMessage = null
                    )
                }
                is GarminResult.Error -> {
                    if (fullResult.message.contains("Not authenticated") ||
                        fullResult.message.contains("Session expired")) {
                        // Try to re-authenticate
                        when (val reAuth = repository.autoLogin()) {
                            is GarminAuthResult.Success -> {
                                refreshData() // Retry
                            }
                            is GarminAuthResult.Error -> {
                                _uiState.value = MainUiState(
                                    isLoggedIn = false,
                                    errorMessage = "Session expired. Please login again."
                                )
                            }
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = fullResult.message
                        )
                    }
                }
            }
        }
    }

    fun exportToJson(): File? {
        val data = fullHealthData ?: return null

        return try {
            val json = repository.exportToJson(data)
            val fileName = "garmin_health_${data.date}.json"
            val file = File(context.cacheDir, fileName)
            file.writeText(json)

            _uiState.value = _uiState.value.copy(exportedFile = file)
            file
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to export: ${e.message}"
            )
            null
        }
    }

    fun shareJson(context: Context) {
        val file = exportToJson() ?: return

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share Health Data"))
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to share: ${e.message}"
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearExportedFile() {
        _uiState.value = _uiState.value.copy(exportedFile = null)
    }
}
