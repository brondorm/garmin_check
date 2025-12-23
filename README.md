# Garmin Connect Health Dashboard

A mobile-first health data dashboard that retrieves data from Garmin Connect using the unofficial python-garminconnect library.

## Architecture

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│  Android App    │ ──── │  FastAPI Backend │ ──── │ Garmin Connect  │
│ (Jetpack Compose)│ REST │   (Python 3.11)  │      │   (Unofficial)  │
└─────────────────┘      └──────────────────┘      └─────────────────┘
```

## Features

- **Secure Authentication**: Credentials stored using Android EncryptedSharedPreferences
- **Real-time Health Data**:
  - HRV (Heart Rate Variability) with status
  - Heart Rate (resting, min, max, average)
  - Sleep metrics (score, deep/REM/light stages)
  - Stress levels
  - Body Battery
  - Step count
- **Dark Theme UI**: Garmin-inspired minimalist design
- **Pull to Refresh**: Update data on demand
- **Error Handling**: Network errors, authentication failures

## Tech Stack

### Backend
- Python 3.11+
- FastAPI
- python-garminconnect
- Pydantic
- Docker

### Android
- Kotlin
- Jetpack Compose
- Hilt (Dependency Injection)
- Retrofit + OkHttp
- Coroutines + Flow
- EncryptedSharedPreferences

## Quick Start

### 1. Deploy Backend

#### Using Docker (Recommended)

```bash
# Clone the repository
git clone https://github.com/yourusername/garmin_check.git
cd garmin_check

# Start the backend
docker-compose up -d

# Check logs
docker-compose logs -f garmin-api
```

#### Manual Installation

```bash
cd backend

# Create virtual environment
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run the server
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 2. Test API

```bash
# Health check
curl http://localhost:8000/

# Login
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "your@email.com", "password": "yourpassword"}'

# Get dashboard data (with token)
curl http://localhost:8000/api/v1/health/dashboard \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 3. Build Android App

#### Prerequisites
- Android Studio Hedgehog or later
- JDK 17+
- Android SDK 34

#### Configuration

1. Open `android/` folder in Android Studio

2. Update API URL in `app/build.gradle.kts`:
   ```kotlin
   // For emulator (default)
   buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000\"")

   // For production
   buildConfigField("String", "API_BASE_URL", "\"https://your-server.com\"")
   ```

3. Build and run:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Health check |
| POST | `/api/v1/auth/login` | Authenticate with Garmin |
| POST | `/api/v1/auth/logout` | Logout |
| GET | `/api/v1/health/dashboard` | Get dashboard data |
| GET | `/api/v1/health/full` | Get complete health data |
| POST | `/api/v1/health/fetch` | Fetch data (direct auth) |

### Request/Response Examples

#### Login
```json
// Request
POST /api/v1/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}

// Response
{
  "success": true,
  "message": "Authentication successful",
  "token": "abc123..."
}
```

#### Dashboard Data
```json
// Response
{
  "date": "2024-01-15",
  "fetched_at": "2024-01-15T10:30:00",
  "hrv_status": "BALANCED",
  "hrv_value": 45,
  "hrv_weekly_avg": 42,
  "resting_hr": 58,
  "min_hr": 52,
  "max_hr": 145,
  "sleep_score": 82,
  "total_sleep_hours": 7.5,
  "stress_level": 28,
  "body_battery_high": 95,
  "body_battery_low": 25,
  "total_steps": 8543
}
```

## Production Deployment

### Backend with HTTPS (nginx)

1. Get SSL certificate (Let's Encrypt):
   ```bash
   certbot certonly --standalone -d your-domain.com
   ```

2. Create `nginx.conf`:
   ```nginx
   server {
       listen 443 ssl;
       server_name your-domain.com;

       ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
       ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

       location / {
           proxy_pass http://garmin-api:8000;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```

3. Uncomment nginx service in `docker-compose.yml`

### Security Considerations

- **Never commit credentials** - Use environment variables
- **Use HTTPS in production** - SSL/TLS is required for mobile apps
- **Session tokens expire** after 24 hours
- **Garmin may block** frequent authentication attempts

## Project Structure

```
garmin_check/
├── backend/
│   ├── app/
│   │   ├── __init__.py
│   │   ├── main.py          # FastAPI app
│   │   ├── garmin_client.py # Garmin Connect wrapper
│   │   └── models.py        # Pydantic models
│   ├── requirements.txt
│   ├── Dockerfile
│   └── .env.example
├── android/
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/garmincheck/app/
│   │       │   ├── data/           # API & Repository
│   │       │   ├── di/             # Hilt modules
│   │       │   ├── ui/             # Compose screens
│   │       │   ├── MainActivity.kt
│   │       │   └── GarminCheckApp.kt
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── docker-compose.yml
└── README.md
```

## Troubleshooting

### "Invalid credentials" error
- Verify your Garmin Connect email/password
- Try logging into connect.garmin.com manually first
- Wait a few minutes if you've made too many attempts

### "Connection error" on Android
- Check if backend is running: `curl http://your-server:8000/`
- For emulator, use `10.0.2.2` instead of `localhost`
- Ensure `android:usesCleartextTraffic="true"` for HTTP (dev only)

### No data returned
- Some metrics require a Garmin device that tracks them
- Data may not be available for the current day yet
- Try specifying a previous date: `?target_date=2024-01-14`

## Disclaimer

This project uses the unofficial python-garminconnect library. It is not affiliated with or endorsed by Garmin. Use at your own risk. Garmin may change their API at any time, which could break this integration.

## License

MIT License - See LICENSE file for details.
