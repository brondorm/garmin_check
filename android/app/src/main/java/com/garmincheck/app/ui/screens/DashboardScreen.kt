package com.garmincheck.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.garmincheck.app.data.model.DashboardData
import com.garmincheck.app.ui.components.*
import com.garmincheck.app.ui.theme.GarminColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    data: DashboardData?,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dashboard",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = GarminColors.Cyan
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = GarminColors.Cyan
                            )
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = GarminColors.TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GarminColors.Black,
                    titleContentColor = GarminColors.White
                )
            )
        },
        containerColor = GarminColors.Black
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                errorMessage != null && data == null -> {
                    ErrorContent(
                        message = errorMessage,
                        onRetry = onRefresh
                    )
                }
                data != null -> {
                    DashboardContent(
                        data = data,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GarminColors.Cyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    data: DashboardData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date header
        Text(
            text = "Data for ${data.date}",
            style = MaterialTheme.typography.bodySmall,
            color = GarminColors.TextSecondary
        )

        // HRV Card
        HrvCard(
            hrvValue = data.hrvValue,
            hrvStatus = data.hrvStatus,
            weeklyAvg = data.hrvWeeklyAvg,
            feedback = data.hrvFeedback
        )

        // Heart Rate Card
        HeartRateCard(
            restingHr = data.restingHr,
            minHr = data.minHr,
            maxHr = data.maxHr,
            avgHr = data.avgHr
        )

        // Sleep Card
        SleepCard(
            sleepScore = data.sleepScore,
            totalHours = data.totalSleepHours,
            deepMinutes = data.deepSleepMinutes,
            remMinutes = data.remSleepMinutes,
            lightMinutes = data.lightSleepMinutes
        )

        // Stress Card
        StressCard(
            stressLevel = data.stressLevel,
            avgStress = data.avgStress
        )

        // Body Battery & Steps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BodyBatteryCard(
                high = data.bodyBatteryHigh,
                low = data.bodyBatteryLow,
                modifier = Modifier.weight(1f)
            )
            StepsCard(
                steps = data.totalSteps,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HrvCard(
    hrvValue: Int,
    hrvStatus: String,
    weeklyAvg: Int,
    feedback: String
) {
    val statusColor = when (hrvStatus.uppercase()) {
        "BALANCED" -> GarminColors.Cyan
        "LOW" -> GarminColors.Orange
        "POOR" -> GarminColors.Red
        else -> GarminColors.TextSecondary
    }

    MetricCard(
        title = "HRV",
        icon = Icons.Default.MonitorHeart,
        accentColor = GarminColors.Purple
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricValue(
                value = hrvValue.toString(),
                unit = "ms",
                label = "Last night"
            )
            StatusBadge(status = hrvStatus, color = statusColor)
        }
        Spacer(modifier = Modifier.height(12.dp))
        MetricRow(label = "Weekly Average", value = "$weeklyAvg ms")
        if (feedback.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feedback,
                style = MaterialTheme.typography.bodySmall,
                color = GarminColors.TextSecondary
            )
        }
    }
}

@Composable
private fun HeartRateCard(
    restingHr: Int,
    minHr: Int,
    maxHr: Int,
    avgHr: Double
) {
    MetricCard(
        title = "Heart Rate",
        icon = Icons.Default.Favorite,
        accentColor = GarminColors.Red
    ) {
        MetricValue(
            value = restingHr.toString(),
            unit = "bpm",
            label = "Resting",
            valueColor = GarminColors.Red
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetricRow(label = "Min", value = "$minHr bpm")
        Spacer(modifier = Modifier.height(4.dp))
        MetricRow(label = "Max", value = "$maxHr bpm")
        Spacer(modifier = Modifier.height(4.dp))
        MetricRow(label = "Average", value = "${avgHr.toInt()} bpm")
    }
}

@Composable
private fun SleepCard(
    sleepScore: Int,
    totalHours: Double,
    deepMinutes: Int,
    remMinutes: Int,
    lightMinutes: Int
) {
    val scoreColor = when {
        sleepScore >= 80 -> GarminColors.Cyan
        sleepScore >= 60 -> GarminColors.Orange
        else -> GarminColors.Red
    }

    MetricCard(
        title = "Sleep",
        icon = Icons.Default.Bedtime,
        accentColor = GarminColors.Blue
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricValue(
                value = String.format("%.1f", totalHours),
                unit = "hrs",
                label = "Total sleep"
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = sleepScore.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = scoreColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Score",
                    style = MaterialTheme.typography.bodySmall,
                    color = GarminColors.TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        MetricRow(label = "Deep", value = "${deepMinutes}m", valueColor = GarminColors.Blue)
        Spacer(modifier = Modifier.height(4.dp))
        MetricRow(label = "REM", value = "${remMinutes}m", valueColor = GarminColors.Purple)
        Spacer(modifier = Modifier.height(4.dp))
        MetricRow(label = "Light", value = "${lightMinutes}m")
    }
}

@Composable
private fun StressCard(
    stressLevel: Int,
    avgStress: Double
) {
    val stressColor = when {
        stressLevel <= 25 -> GarminColors.Cyan
        stressLevel <= 50 -> GarminColors.Blue
        stressLevel <= 75 -> GarminColors.Orange
        else -> GarminColors.Red
    }

    val stressLabel = when {
        stressLevel <= 25 -> "Rest"
        stressLevel <= 50 -> "Low"
        stressLevel <= 75 -> "Medium"
        else -> "High"
    }

    MetricCard(
        title = "Stress",
        icon = Icons.Default.Psychology,
        accentColor = GarminColors.Orange
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricValue(
                value = stressLevel.toString(),
                label = "Current level",
                valueColor = stressColor
            )
            StatusBadge(status = stressLabel, color = stressColor)
        }
        Spacer(modifier = Modifier.height(12.dp))
        MetricRow(label = "Average", value = "${avgStress.toInt()}")
    }
}

@Composable
private fun BodyBatteryCard(
    high: Int,
    low: Int,
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "Body Battery",
        icon = Icons.Default.BatteryChargingFull,
        accentColor = GarminColors.Cyan,
        modifier = modifier
    ) {
        MetricValue(
            value = "$low-$high",
            label = "Range",
            valueColor = GarminColors.Cyan
        )
    }
}

@Composable
private fun StepsCard(
    steps: Int,
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "Steps",
        icon = Icons.Default.DirectionsWalk,
        accentColor = GarminColors.Cyan,
        modifier = modifier
    ) {
        MetricValue(
            value = String.format("%,d", steps),
            label = "Today",
            valueColor = GarminColors.Cyan
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = GarminColors.Red,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = GarminColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = GarminColors.Cyan,
                contentColor = GarminColors.Black
            )
        ) {
            Text("Retry")
        }
    }
}
