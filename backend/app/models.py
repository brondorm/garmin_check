"""
Pydantic models for API request/response validation.
"""

from pydantic import BaseModel, EmailStr, Field
from typing import Optional
from datetime import date


class LoginRequest(BaseModel):
    """Request model for user login."""
    email: EmailStr
    password: str = Field(..., min_length=1)


class LoginResponse(BaseModel):
    """Response model for successful login."""
    success: bool
    message: str
    token: Optional[str] = None


class StatsData(BaseModel):
    """Daily statistics data model."""
    total_steps: int = 0
    total_distance_meters: float = 0
    active_calories: int = 0
    total_calories: int = 0
    resting_heart_rate: int = 0
    min_heart_rate: int = 0
    max_heart_rate: int = 0
    average_stress_level: int = 0
    floors_ascended: int = 0
    floors_descended: int = 0
    intensity_minutes: int = 0
    body_battery_high: int = 0
    body_battery_low: int = 0


class SleepData(BaseModel):
    """Sleep data model."""
    sleep_start: Optional[str] = None
    sleep_end: Optional[str] = None
    total_sleep_seconds: int = 0
    total_sleep_minutes: int = 0
    deep_sleep_minutes: int = 0
    light_sleep_minutes: int = 0
    rem_sleep_minutes: int = 0
    awake_minutes: int = 0
    sleep_score: int = 0
    average_spo2: Optional[float] = None
    lowest_spo2: Optional[float] = None
    average_respiration: Optional[float] = None


class StressData(BaseModel):
    """Stress data model."""
    overall_stress_level: int = 0
    average_stress: float = 0
    max_stress: int = 0
    min_stress: int = 0
    stress_duration_minutes: int = 0
    rest_stress_duration_minutes: int = 0
    low_stress_duration_minutes: int = 0
    medium_stress_duration_minutes: int = 0
    high_stress_duration_minutes: int = 0


class HeartRateData(BaseModel):
    """Heart rate data model."""
    resting_heart_rate: int = 0
    min_heart_rate: int = 0
    max_heart_rate: int = 0
    average_heart_rate: float = 0
    last_seven_days_resting_hr: int = 0


class HrvData(BaseModel):
    """HRV (Heart Rate Variability) data model."""
    weekly_average: int = 0
    last_night: int = 0
    last_night_average: int = 0
    last_night_5_min_high: int = 0
    baseline_low: int = 0
    baseline_balanced_low: int = 0
    baseline_balanced_upper: int = 0
    status: str = "UNKNOWN"
    feedback: str = ""


class HealthDataResponse(BaseModel):
    """Complete health data response model."""
    date: str
    fetched_at: str
    stats: StatsData
    sleep: SleepData
    stress: StressData
    heart_rate: HeartRateData
    hrv: HrvData


class DashboardData(BaseModel):
    """Simplified dashboard data for mobile app."""
    date: str
    fetched_at: str

    # HRV Card
    hrv_status: str
    hrv_value: int
    hrv_weekly_avg: int
    hrv_feedback: str

    # Heart Rate Card
    resting_hr: int
    min_hr: int
    max_hr: int
    avg_hr: float

    # Sleep Card
    sleep_score: int
    total_sleep_hours: float
    deep_sleep_minutes: int
    rem_sleep_minutes: int
    light_sleep_minutes: int

    # Stress Card
    stress_level: int
    avg_stress: float

    # Body Battery
    body_battery_high: int
    body_battery_low: int

    # Steps
    total_steps: int


class ErrorResponse(BaseModel):
    """Error response model."""
    error: str
    detail: Optional[str] = None


class DataRequest(BaseModel):
    """Request model for fetching data with optional date."""
    email: EmailStr
    password: str = Field(..., min_length=1)
    date: Optional[str] = Field(None, pattern=r"^\d{4}-\d{2}-\d{2}$")
