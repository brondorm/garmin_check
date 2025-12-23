# Garmin Connect Health Dashboard

A standalone Android app that fetches health data directly from your Garmin Connect account and exports it to JSON. No backend server required!

## Architecture

```
┌─────────────────────┐                ┌─────────────────┐
│   Android App       │ ──── HTTPS ──→ │ Garmin Connect  │
│  (Jetpack Compose)  │                │   (SSO Auth)    │
└─────────────────────┘                └─────────────────┘
         │
         ↓
    📄 JSON Export
```

## Features

- **Direct Garmin Connect Integration**: No backend server needed - the app communicates directly with Garmin Connect
- **Secure Authentication**: Credentials stored using Android EncryptedSharedPreferences (AES-256)
- **Auto-login**: Remembers your credentials for seamless experience
- **Real-time Health Data**:
  - HRV (Heart Rate Variability) with status indicator
  - Heart Rate (resting, min, max, average)
  - Sleep metrics (score, deep/REM/light stages)
  - Stress levels
  - Body Battery
  - Step count
- **JSON Export**: Share/export your health data as JSON file
- **Dark Theme UI**: Garmin-inspired minimalist design
- **Pull to Refresh**: Update data on demand
- **Error Handling**: Network errors, invalid credentials, session expiry

## Screenshots

The app features a dark theme inspired by Garmin's design language:

- **Login Screen**: Clean email/password form with Garmin branding
- **Dashboard**: Cards showing HRV, Heart Rate, Sleep, Stress, Body Battery, and Steps
- **Export**: One-tap JSON export via Android share sheet

## Tech Stack

- **Kotlin** - Modern Android development
- **Jetpack Compose** - Declarative UI
- **Hilt** - Dependency Injection
- **OkHttp** - HTTP client for Garmin SSO authentication
- **Gson** - JSON serialization
- **Coroutines + Flow** - Async operations
- **EncryptedSharedPreferences** - Secure credential storage

## Quick Start

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK 34
- Android device/emulator with Android 8.0+ (API 26+)

### Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/garmin_check.git
   cd garmin_check
   ```

2. Open `android/` folder in Android Studio

3. Sync Gradle and build:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

4. Run on device/emulator

5. Login with your Garmin Connect credentials

## Usage

### Login
1. Enter your Garmin Connect email and password
2. Tap "Sign In"
3. Credentials are securely stored for auto-login

### View Health Data
- Dashboard shows today's health metrics
- Tap refresh icon to update data
- Data is fetched directly from Garmin Connect

### Export to JSON
1. Tap the download icon in the top bar
2. Choose how to share (save to files, send via messenger, etc.)
3. JSON file includes all raw health data

### Logout
- Tap the logout icon to clear credentials and session

## Exported JSON Format

```json
{
  "date": "2024-01-15",
  "fetchedAt": "2024-01-15T10:30:00",
  "stats": {
    "totalSteps": 8543,
    "totalDistanceMeters": 6234.5,
    "activeCalories": 450,
    "restingHeartRate": 58,
    "bodyBatteryHigh": 95,
    "bodyBatteryLow": 25
  },
  "sleep": {
    "dailySleep": {
      "totalSleepSeconds": 27000,
      "deepSleepSeconds": 5400,
      "lightSleepSeconds": 14400,
      "remSleepSeconds": 7200,
      "sleepScores": {
        "overall": { "value": 82 }
      }
    }
  },
  "heartRate": {
    "restingHeartRate": 58,
    "minHeartRate": 52,
    "maxHeartRate": 145
  },
  "stress": {
    "overallStressLevel": 28,
    "lowStressDuration": 420,
    "mediumStressDuration": 180
  },
  "hrv": {
    "hrvSummary": {
      "weeklyAvg": 42,
      "lastNight": 45,
      "status": "BALANCED"
    }
  }
}
```

## Project Structure

```
garmin_check/
└── android/
    ├── app/
    │   └── src/main/
    │       ├── java/com/garmincheck/app/
    │       │   ├── data/
    │       │   │   ├── garmin/           # Garmin Connect client
    │       │   │   │   ├── GarminConnectClient.kt
    │       │   │   │   └── GarminModels.kt
    │       │   │   └── repository/       # Data repositories
    │       │   │       ├── CredentialsRepository.kt
    │       │   │       └── GarminRepository.kt
    │       │   ├── di/                   # Hilt DI modules
    │       │   ├── ui/
    │       │   │   ├── components/       # Reusable UI components
    │       │   │   ├── screens/          # Login & Dashboard screens
    │       │   │   └── theme/            # Garmin dark theme
    │       │   ├── MainActivity.kt
    │       │   └── GarminCheckApp.kt
    │       ├── res/
    │       └── AndroidManifest.xml
    ├── build.gradle.kts
    └── settings.gradle.kts
```

## Troubleshooting

### "Invalid email or password"
- Verify your Garmin Connect credentials at [connect.garmin.com](https://connect.garmin.com)
- Garmin may temporarily block login after too many failed attempts - wait a few minutes

### "Two-factor authentication is enabled"
- The app doesn't support 2FA/MFA yet
- Temporarily disable 2FA in your Garmin account settings to use this app

### "Login failed"
- Check your internet connection
- Garmin's SSO servers may be temporarily unavailable
- Try again in a few minutes

### No HRV/Sleep data
- Some metrics require specific Garmin devices (e.g., HRV needs compatible watches)
- Data may not be synced from your device yet
- Some data is only available after a full night's sleep

### App crashes on login
- Clear app data and try again
- Make sure you're running Android 8.0 or later

## Security Notes

- **Credentials are encrypted** using Android's EncryptedSharedPreferences (AES-256-GCM)
- **No data is sent to third parties** - the app only communicates with Garmin's servers
- **Session cookies** are stored in memory only and cleared on logout
- **Exported JSON** may contain sensitive health data - share responsibly

## Known Limitations

- No support for two-factor authentication (2FA/MFA)
- Garmin may change their authentication flow, which could break the app
- Some health metrics require specific Garmin devices
- Rate limiting: too many requests may result in temporary blocks

## Disclaimer

This project uses unofficial Garmin Connect API endpoints. It is **not affiliated with or endorsed by Garmin**. Use at your own risk. Garmin may change their API at any time, which could break this integration.

The app is intended for personal use to access your own health data.

## License

MIT License - See LICENSE file for details.
