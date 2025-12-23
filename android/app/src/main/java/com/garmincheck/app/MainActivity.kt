package com.garmincheck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.garmincheck.app.ui.screens.DashboardScreen
import com.garmincheck.app.ui.screens.LoginScreen
import com.garmincheck.app.ui.screens.MainViewModel
import com.garmincheck.app.ui.theme.GarminCheckTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GarminCheckTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                if (uiState.isLoggedIn) {
                    DashboardScreen(
                        data = uiState.dashboardData,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onRefresh = viewModel::refreshData,
                        onLogout = viewModel::logout,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LoginScreen(
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onLogin = viewModel::login,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
