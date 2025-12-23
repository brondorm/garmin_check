package com.garmincheck.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garmincheck.app.data.model.ApiResult
import com.garmincheck.app.data.model.DashboardData
import com.garmincheck.app.data.repository.GarminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val dashboardData: DashboardData? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: GarminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        val isLoggedIn = repository.isLoggedIn()
        _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)

        if (isLoggedIn) {
            refreshData()
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            when (val result = repository.login(email, password)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        isLoading = false
                    )
                    refreshData()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = MainUiState(isLoggedIn = false)
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            when (val result = repository.getDashboardData()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        dashboardData = result.data,
                        errorMessage = null
                    )
                }
                is ApiResult.Error -> {
                    if (result.code == 401) {
                        _uiState.value = MainUiState(isLoggedIn = false)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
