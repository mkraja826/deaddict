package com.deaddict.app.privacy

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.privacyDataStore by preferencesDataStore("privacy_preferences")

data class PrivacyPreferences(
    val biometricLockEnabled: Boolean = false,
    val screenProtectionEnabled: Boolean = false,
    val analyticsEnabled: Boolean = false,
    val usageMonitoringEnabled: Boolean = false,
)

class PrivacyPreferenceStore(private val context: Context) {
    val preferences: Flow<PrivacyPreferences> = context.privacyDataStore.data.map { values ->
        PrivacyPreferences(
            biometricLockEnabled = values[BIOMETRIC_LOCK] ?: false,
            screenProtectionEnabled = values[SCREEN_PROTECTION] ?: false,
            analyticsEnabled = values[ANALYTICS] ?: false,
            usageMonitoringEnabled = values[USAGE_MONITORING] ?: false,
        )
    }

    suspend fun setBiometricLock(enabled: Boolean) = set(BIOMETRIC_LOCK, enabled)
    suspend fun setScreenProtection(enabled: Boolean) = set(SCREEN_PROTECTION, enabled)
    suspend fun setAnalytics(enabled: Boolean) = set(ANALYTICS, enabled)
    suspend fun setUsageMonitoring(enabled: Boolean) = set(USAGE_MONITORING, enabled)

    suspend fun clear() {
        context.privacyDataStore.edit { it.clear() }
    }

    private suspend fun set(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        context.privacyDataStore.edit { it[key] = value }
    }

    private companion object {
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val SCREEN_PROTECTION = booleanPreferencesKey("screen_and_recent_preview_protection")
        val ANALYTICS = booleanPreferencesKey("analytics_enabled")
        val USAGE_MONITORING = booleanPreferencesKey("usage_monitoring_enabled")
    }
}
