package com.garmincheck.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.garmincheck.app.ui.screens.DashboardScreen
import com.garmincheck.app.ui.screens.LoginScreen
import com.garmincheck.app.ui.screens.MainViewModel
import com.garmincheck.app.ui.theme.GarminCheckTheme
import com.garmincheck.app.ui.theme.GarminColors
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
                val context = LocalContext.current

                // Show toast when file is exported
                LaunchedEffect(uiState.exportedFile) {
                    uiState.exportedFile?.let { file ->
                        Toast.makeText(
                            context,
                            "Exported to ${file.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.clearExportedFile()
                    }
                }

                when {
                    // Auto-login in progress
                    uiState.isAutoLogging -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(GarminColors.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = GarminColors.Cyan)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Connecting to Garmin...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GarminColors.TextSecondary
                                )
                            }
                        }
                    }
                    // Logged in - show dashboard
                    uiState.isLoggedIn -> {
                        DashboardScreen(
                            data = uiState.dashboardData,
                            isLoading = uiState.isLoading,
                            errorMessage = uiState.errorMessage,
                            userName = uiState.userName,
                            onRefresh = viewModel::refreshData,
                            onExport = { viewModel.shareJson(context) },
                            onLogout = viewModel::logout,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Not logged in - show login
                    else -> {
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
}
