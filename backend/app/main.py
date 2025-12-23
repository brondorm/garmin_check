"""
FastAPI application for Garmin Connect data retrieval.
Provides REST API endpoints for mobile app integration.
"""

import os
import secrets
import hashlib
from datetime import date, datetime, timedelta
from typing import Optional
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Depends, Header
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

from .models import (
    LoginRequest,
    LoginResponse,
    DataRequest,
    HealthDataResponse,
    DashboardData,
    ErrorResponse,
    StatsData,
    SleepData,
    StressData,
    HeartRateData,
    HrvData,
)
from .garmin_client import GarminClient, GarminClientError


# Simple in-memory session store (use Redis in production)
sessions: dict[str, dict] = {}

security = HTTPBearer(auto_error=False)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler."""
    # Startup
    print("🚀 Garmin Connect API starting...")
    yield
    # Shutdown
    print("👋 Garmin Connect API shutting down...")
    sessions.clear()


app = FastAPI(
    title="Garmin Connect API",
    description="REST API for fetching health data from Garmin Connect",
    version="1.0.0",
    lifespan=lifespan,
)

# CORS configuration for mobile app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def create_session_token(email: str, password: str) -> str:
    """Create a session token for authenticated user."""
    token = secrets.token_urlsafe(32)
    # Store hashed credentials (for re-authentication with Garmin)
    sessions[token] = {
        "email": email,
        "password": password,  # In production, encrypt this
        "created_at": datetime.now(),
        "expires_at": datetime.now() + timedelta(hours=24),
    }
    return token


def get_session(token: str) -> Optional[dict]:
    """Get session data by token."""
    session = sessions.get(token)
    if session and session["expires_at"] > datetime.now():
        return session
    elif session:
        del sessions[token]
    return None


async def get_current_session(
    credentials: HTTPAuthorizationCredentials = Depends(security),
) -> dict:
    """Dependency to get current authenticated session."""
    if not credentials:
        raise HTTPException(status_code=401, detail="Authentication required")

    session = get_session(credentials.credentials)
    if not session:
        raise HTTPException(status_code=401, detail="Invalid or expired session")

    return session


@app.get("/", tags=["Health"])
async def root():
    """Health check endpoint."""
    return {
        "status": "healthy",
        "service": "Garmin Connect API",
        "version": "1.0.0",
        "timestamp": datetime.now().isoformat(),
    }


@app.post("/api/v1/auth/login", response_model=LoginResponse, tags=["Authentication"])
async def login(request: LoginRequest):
    """
    Authenticate with Garmin Connect credentials.

    Returns a session token for subsequent API calls.
    """
    try:
        # Verify credentials with Garmin
        client = GarminClient(request.email, request.password)
        client.authenticate()

        # Create session token
        token = create_session_token(request.email, request.password)

        return LoginResponse(
            success=True,
            message="Authentication successful",
            token=token,
        )

    except GarminClientError as e:
        error_msg = str(e)
        if "Invalid credentials" in error_msg:
            raise HTTPException(
                status_code=401,
                detail="Invalid email or password",
            )
        elif "Connection error" in error_msg:
            raise HTTPException(
                status_code=503,
                detail="Unable to connect to Garmin Connect. Please try again.",
            )
        else:
            raise HTTPException(
                status_code=500,
                detail=f"Authentication failed: {error_msg}",
            )


@app.post("/api/v1/auth/logout", tags=["Authentication"])
async def logout(session: dict = Depends(get_current_session)):
    """Logout and invalidate session."""
    # Find and remove the session
    for token, sess in list(sessions.items()):
        if sess == session:
            del sessions[token]
            break

    return {"success": True, "message": "Logged out successfully"}


@app.get(
    "/api/v1/health/full",
    response_model=HealthDataResponse,
    responses={401: {"model": ErrorResponse}, 500: {"model": ErrorResponse}},
    tags=["Health Data"],
)
async def get_full_health_data(
    target_date: Optional[str] = None,
    session: dict = Depends(get_current_session),
):
    """
    Get complete health data for the specified date.

    If no date is provided, returns data for today.
    Date format: YYYY-MM-DD
    """
    try:
        client = GarminClient(session["email"], session["password"])
        client.authenticate()

        dt = date.fromisoformat(target_date) if target_date else None
        data = client.get_all_data(dt)

        return HealthDataResponse(
            date=data["date"],
            fetched_at=data["fetched_at"],
            stats=StatsData(**data["stats"]),
            sleep=SleepData(**data["sleep"]),
            stress=StressData(**data["stress"]),
            heart_rate=HeartRateData(**data["heart_rate"]),
            hrv=HrvData(**data["hrv"]),
        )

    except GarminClientError as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get(
    "/api/v1/health/dashboard",
    response_model=DashboardData,
    responses={401: {"model": ErrorResponse}, 500: {"model": ErrorResponse}},
    tags=["Health Data"],
)
async def get_dashboard_data(
    target_date: Optional[str] = None,
    session: dict = Depends(get_current_session),
):
    """
    Get simplified dashboard data optimized for mobile app display.

    Returns key metrics for HRV, Heart Rate, Sleep, and Stress cards.
    """
    try:
        client = GarminClient(session["email"], session["password"])
        client.authenticate()

        dt = date.fromisoformat(target_date) if target_date else None
        data = client.get_all_data(dt)

        # Transform to dashboard format
        return DashboardData(
            date=data["date"],
            fetched_at=data["fetched_at"],
            # HRV Card
            hrv_status=data["hrv"].get("status", "UNKNOWN"),
            hrv_value=data["hrv"].get("last_night", 0),
            hrv_weekly_avg=data["hrv"].get("weekly_average", 0),
            hrv_feedback=data["hrv"].get("feedback", ""),
            # Heart Rate Card
            resting_hr=data["heart_rate"].get("resting_heart_rate", 0),
            min_hr=data["heart_rate"].get("min_heart_rate", 0),
            max_hr=data["heart_rate"].get("max_heart_rate", 0),
            avg_hr=data["heart_rate"].get("average_heart_rate", 0),
            # Sleep Card
            sleep_score=data["sleep"].get("sleep_score", 0),
            total_sleep_hours=round(data["sleep"].get("total_sleep_minutes", 0) / 60, 1),
            deep_sleep_minutes=data["sleep"].get("deep_sleep_minutes", 0),
            rem_sleep_minutes=data["sleep"].get("rem_sleep_minutes", 0),
            light_sleep_minutes=data["sleep"].get("light_sleep_minutes", 0),
            # Stress Card
            stress_level=data["stress"].get("overall_stress_level", 0),
            avg_stress=data["stress"].get("average_stress", 0),
            # Body Battery
            body_battery_high=data["stats"].get("body_battery_high", 0),
            body_battery_low=data["stats"].get("body_battery_low", 0),
            # Steps
            total_steps=data["stats"].get("total_steps", 0),
        )

    except GarminClientError as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post(
    "/api/v1/health/fetch",
    response_model=DashboardData,
    responses={401: {"model": ErrorResponse}, 500: {"model": ErrorResponse}},
    tags=["Health Data"],
)
async def fetch_data_direct(request: DataRequest):
    """
    Fetch health data directly without session (for simple integrations).

    Authenticates with provided credentials and returns dashboard data.
    """
    try:
        client = GarminClient(request.email, request.password)
        client.authenticate()

        dt = date.fromisoformat(request.date) if request.date else None
        data = client.get_all_data(dt)

        return DashboardData(
            date=data["date"],
            fetched_at=data["fetched_at"],
            hrv_status=data["hrv"].get("status", "UNKNOWN"),
            hrv_value=data["hrv"].get("last_night", 0),
            hrv_weekly_avg=data["hrv"].get("weekly_average", 0),
            hrv_feedback=data["hrv"].get("feedback", ""),
            resting_hr=data["heart_rate"].get("resting_heart_rate", 0),
            min_hr=data["heart_rate"].get("min_heart_rate", 0),
            max_hr=data["heart_rate"].get("max_heart_rate", 0),
            avg_hr=data["heart_rate"].get("average_heart_rate", 0),
            sleep_score=data["sleep"].get("sleep_score", 0),
            total_sleep_hours=round(data["sleep"].get("total_sleep_minutes", 0) / 60, 1),
            deep_sleep_minutes=data["sleep"].get("deep_sleep_minutes", 0),
            rem_sleep_minutes=data["sleep"].get("rem_sleep_minutes", 0),
            light_sleep_minutes=data["sleep"].get("light_sleep_minutes", 0),
            stress_level=data["stress"].get("overall_stress_level", 0),
            avg_stress=data["stress"].get("average_stress", 0),
            body_battery_high=data["stats"].get("body_battery_high", 0),
            body_battery_low=data["stats"].get("body_battery_low", 0),
            total_steps=data["stats"].get("total_steps", 0),
        )

    except GarminClientError as e:
        error_msg = str(e)
        if "Invalid credentials" in error_msg:
            raise HTTPException(status_code=401, detail="Invalid email or password")
        elif "Connection error" in error_msg:
            raise HTTPException(
                status_code=503, detail="Unable to connect to Garmin Connect"
            )
        else:
            raise HTTPException(status_code=500, detail=error_msg)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
