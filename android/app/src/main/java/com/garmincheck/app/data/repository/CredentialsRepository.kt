package com.garmincheck.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for securely storing user credentials using EncryptedSharedPreferences
 */
@Singleton
class CredentialsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Save user credentials securely
     */
    fun saveCredentials(email: String, password: String) {
        sharedPreferences.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    /**
     * Get stored email
     */
    fun getEmail(): String? = sharedPreferences.getString(KEY_EMAIL, null)

    /**
     * Get stored password
     */
    fun getPassword(): String? = sharedPreferences.getString(KEY_PASSWORD, null)

    /**
     * Check if user has stored credentials
     */
    fun hasCredentials(): Boolean {
        return !getEmail().isNullOrEmpty() && !getPassword().isNullOrEmpty()
    }

    /**
     * Clear all stored credentials
     */
    fun clearCredentials() {
        sharedPreferences.edit()
            .remove(KEY_EMAIL)
            .remove(KEY_PASSWORD)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "encrypted_credentials"
        private const val KEY_EMAIL = "garmin_email"
        private const val KEY_PASSWORD = "garmin_password"
    }
}
