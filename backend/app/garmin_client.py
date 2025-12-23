"""
Garmin Connect client wrapper using python-garminconnect library.
Handles authentication and data fetching from Garmin Connect.
"""

import os
import json
from datetime import date, datetime
from typing import Optional
from garminconnect import Garmin, GarminConnectAuthenticationError, GarminConnectConnectionError


class GarminClientError(Exception):
    """Custom exception for Garmin client errors."""
    pass


class GarminClient:
    """Wrapper for Garmin Connect API interactions."""

    def __init__(self, email: Optional[str] = None, password: Optional[str] = None):
        self.email = email or os.getenv("GARMIN_EMAIL")
        self.password = password or os.getenv("GARMIN_PASSWORD")
        self.client: Optional[Garmin] = None
        self._authenticated = False

    def authenticate(self) -> bool:
        """
        Authenticate with Garmin Connect.

        Returns:
            bool: True if authentication successful

        Raises:
            GarminClientError: If authentication fails
        """
        if not self.email or not self.password:
            raise GarminClientError("Email and password are required")

        try:
            self.client = Garmin(self.email, self.password)
            self.client.login()
            self._authenticated = True
            return True
        except GarminConnectAuthenticationError as e:
            raise GarminClientError(f"Authentication failed: Invalid credentials") from e
        except GarminConnectConnectionError as e:
            raise GarminClientError(f"Connection error: Unable to reach Garmin Connect") from e
        except Exception as e:
            raise GarminClientError(f"Unexpected error during authentication: {str(e)}") from e

    def _ensure_authenticated(self):
        """Ensure client is authenticated before making requests."""
        if not self._authenticated or self.client is None:
            raise GarminClientError("Not authenticated. Call authenticate() first.")

    def get_stats(self, target_date: Optional[date] = None) -> dict:
        """
        Get daily statistics for the specified date.

        Args:
            target_date: Date to fetch stats for (defaults to today)

        Returns:
            dict: Daily statistics data
        """
        self._ensure_authenticated()
        target = target_date or date.today()

        try:
            stats = self.client.get_stats(target.isoformat())
            return self._clean_stats(stats)
        except Exception as e:
            raise GarminClientError(f"Failed to fetch stats: {str(e)}") from e

    def get_sleep_data(self, target_date: Optional[date] = None) -> dict:
        """
        Get sleep data for the specified date.

        Args:
            target_date: Date to fetch sleep data for (defaults to today)

        Returns:
            dict: Sleep data
        """
        self._ensure_authenticated()
        target = target_date or date.today()

        try:
            sleep = self.client.get_sleep_data(target.isoformat())
            return self._clean_sleep_data(sleep)
        except Exception as e:
            raise GarminClientError(f"Failed to fetch sleep data: {str(e)}") from e

    def get_stress_data(self, target_date: Optional[date] = None) -> dict:
        """
        Get stress data for the specified date.

        Args:
            target_date: Date to fetch stress data for (defaults to today)

        Returns:
            dict: Stress data
        """
        self._ensure_authenticated()
        target = target_date or date.today()

        try:
            stress = self.client.get_stress_data(target.isoformat())
            return self._clean_stress_data(stress)
        except Exception as e:
            raise GarminClientError(f"Failed to fetch stress data: {str(e)}") from e

    def get_heart_rate(self, target_date: Optional[date] = None) -> dict:
        """
        Get heart rate data for the specified date.

        Args:
            target_date: Date to fetch heart rate data for (defaults to today)

        Returns:
            dict: Heart rate data
        """
        self._ensure_authenticated()
        target = target_date or date.today()

        try:
            hr = self.client.get_heart_rates(target.isoformat())
            return self._clean_heart_rate_data(hr)
        except Exception as e:
            raise GarminClientError(f"Failed to fetch heart rate data: {str(e)}") from e

    def get_hrv_data(self, target_date: Optional[date] = None) -> dict:
        """
        Get HRV (Heart Rate Variability) data.

        Args:
            target_date: Date to fetch HRV data for (defaults to today)

        Returns:
            dict: HRV data
        """
        self._ensure_authenticated()
        target = target_date or date.today()

        try:
            hrv = self.client.get_hrv_data(target.isoformat())
            return self._clean_hrv_data(hrv)
        except Exception as e:
            raise GarminClientError(f"Failed to fetch HRV data: {str(e)}") from e

    def get_all_data(self, target_date: Optional[date] = None) -> dict:
        """
        Get all health data for the specified date.

        Args:
            target_date: Date to fetch data for (defaults to today)

        Returns:
            dict: Combined health data
        """
        target = target_date or date.today()

        return {
            "date": target.isoformat(),
            "fetched_at": datetime.now().isoformat(),
            "stats": self.get_stats(target),
            "sleep": self.get_sleep_data(target),
            "stress": self.get_stress_data(target),
            "heart_rate": self.get_heart_rate(target),
            "hrv": self.get_hrv_data(target)
        }

    def _clean_stats(self, data: dict) -> dict:
        """Extract relevant fields from daily stats."""
        if not data:
            return {}

        return {
            "total_steps": data.get("totalSteps", 0),
            "total_distance_meters": data.get("totalDistanceMeters", 0),
            "active_calories": data.get("activeKilocalories", 0),
            "total_calories": data.get("totalKilocalories", 0),
            "resting_heart_rate": data.get("restingHeartRate", 0),
            "min_heart_rate": data.get("minHeartRate", 0),
            "max_heart_rate": data.get("maxHeartRate", 0),
            "average_stress_level": data.get("averageStressLevel", 0),
            "floors_ascended": data.get("floorsAscended", 0),
            "floors_descended": data.get("floorsDescended", 0),
            "intensity_minutes": data.get("intensityMinutesGoal", 0),
            "body_battery_high": data.get("bodyBatteryChargedValue", 0),
            "body_battery_low": data.get("bodyBatteryDrainedValue", 0),
        }

    def _clean_sleep_data(self, data: dict) -> dict:
        """Extract relevant fields from sleep data."""
        if not data:
            return {}

        daily_sleep = data.get("dailySleepDTO", {})

        sleep_levels = {}
        if "sleepLevels" in data:
            for level in data.get("sleepLevels", []):
                level_name = level.get("activityLevel", "unknown").lower()
                sleep_levels[level_name] = level.get("seconds", 0) // 60  # Convert to minutes

        return {
            "sleep_start": daily_sleep.get("sleepStartTimestampLocal"),
            "sleep_end": daily_sleep.get("sleepEndTimestampLocal"),
            "total_sleep_seconds": daily_sleep.get("sleepTimeSeconds", 0),
            "total_sleep_minutes": (daily_sleep.get("sleepTimeSeconds", 0) // 60),
            "deep_sleep_minutes": daily_sleep.get("deepSleepSeconds", 0) // 60,
            "light_sleep_minutes": daily_sleep.get("lightSleepSeconds", 0) // 60,
            "rem_sleep_minutes": daily_sleep.get("remSleepSeconds", 0) // 60,
            "awake_minutes": daily_sleep.get("awakeSleepSeconds", 0) // 60,
            "sleep_score": daily_sleep.get("sleepScores", {}).get("overall", {}).get("value", 0),
            "average_spo2": data.get("averageSpO2Value"),
            "lowest_spo2": data.get("lowestSpO2Value"),
            "average_respiration": data.get("averageRespirationValue"),
        }

    def _clean_stress_data(self, data: dict) -> dict:
        """Extract relevant fields from stress data."""
        if not data:
            return {}

        # Get summary values
        overall = data.get("overallStressLevel", 0)
        rest_stress = data.get("restStressValuesArray", [])
        activity_stress = data.get("activityStressValuesArray", [])

        # Calculate averages
        all_stress = [v for v in rest_stress + activity_stress if v and v > 0]
        avg_stress = sum(all_stress) / len(all_stress) if all_stress else 0

        return {
            "overall_stress_level": overall,
            "average_stress": round(avg_stress, 1),
            "max_stress": max(all_stress) if all_stress else 0,
            "min_stress": min(all_stress) if all_stress else 0,
            "stress_duration_minutes": data.get("stressDuration", 0),
            "rest_stress_duration_minutes": data.get("restStressDuration", 0),
            "low_stress_duration_minutes": data.get("lowStressDuration", 0),
            "medium_stress_duration_minutes": data.get("mediumStressDuration", 0),
            "high_stress_duration_minutes": data.get("highStressDuration", 0),
        }

    def _clean_heart_rate_data(self, data: dict) -> dict:
        """Extract relevant fields from heart rate data."""
        if not data:
            return {}

        hr_values = data.get("heartRateValues", [])
        valid_hr = [v[1] for v in hr_values if v and len(v) > 1 and v[1] and v[1] > 0]

        return {
            "resting_heart_rate": data.get("restingHeartRate", 0),
            "min_heart_rate": data.get("minHeartRate", 0),
            "max_heart_rate": data.get("maxHeartRate", 0),
            "average_heart_rate": round(sum(valid_hr) / len(valid_hr), 1) if valid_hr else 0,
            "last_seven_days_resting_hr": data.get("lastSevenDaysAvgRestingHeartRate", 0),
        }

    def _clean_hrv_data(self, data: dict) -> dict:
        """Extract relevant fields from HRV data."""
        if not data:
            return {}

        hrv_summary = data.get("hrvSummary", {})

        return {
            "weekly_average": hrv_summary.get("weeklyAvg", 0),
            "last_night": hrv_summary.get("lastNight", 0),
            "last_night_average": hrv_summary.get("lastNightAvg", 0),
            "last_night_5_min_high": hrv_summary.get("lastNight5MinHigh", 0),
            "baseline_low": hrv_summary.get("baselineLowUpper", 0),
            "baseline_balanced_low": hrv_summary.get("baselineBalancedLow", 0),
            "baseline_balanced_upper": hrv_summary.get("baselineBalancedUpper", 0),
            "status": hrv_summary.get("status", "UNKNOWN"),
            "feedback": hrv_summary.get("feedbackPhrase", ""),
        }


def fetch_garmin_data(email: str, password: str, target_date: Optional[str] = None) -> str:
    """
    Convenience function to fetch all Garmin data and return as JSON string.

    Args:
        email: Garmin account email
        password: Garmin account password
        target_date: Optional date string in YYYY-MM-DD format

    Returns:
        str: JSON string with all health data
    """
    client = GarminClient(email, password)
    client.authenticate()

    dt = date.fromisoformat(target_date) if target_date else None
    data = client.get_all_data(dt)

    return json.dumps(data, indent=2)


if __name__ == "__main__":
    # CLI usage example
    import sys

    email = os.getenv("GARMIN_EMAIL")
    password = os.getenv("GARMIN_PASSWORD")

    if not email or not password:
        print("Error: GARMIN_EMAIL and GARMIN_PASSWORD environment variables required")
        sys.exit(1)

    try:
        result = fetch_garmin_data(email, password)
        print(result)
    except GarminClientError as e:
        print(f"Error: {e}")
        sys.exit(1)
