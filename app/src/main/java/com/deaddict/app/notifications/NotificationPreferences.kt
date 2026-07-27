package com.deaddict.app.notifications

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore by preferencesDataStore("notification_preferences")

data class NotificationPreferences(
    val dailyCheckInEnabled: Boolean = false,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val lastDailyShownAtMillis: Long = 0L,
)

class NotificationPreferenceStore(private val context: Context) {
    val preferences: Flow<NotificationPreferences> =
        context.notificationDataStore.data.map { values ->
            NotificationPreferences(
                dailyCheckInEnabled = values[DAILY_ENABLED] ?: false,
                quietStartHour = values[QUIET_START] ?: 22,
                quietEndHour = values[QUIET_END] ?: 7,
                lastDailyShownAtMillis = values[LAST_DAILY] ?: 0L,
            )
        }

    suspend fun current(): NotificationPreferences = preferences.first()

    suspend fun setDailyEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { it[DAILY_ENABLED] = enabled }
    }

    suspend fun setQuietHours(startHour: Int, endHour: Int) {
        require(startHour in 0..23 && endHour in 0..23)
        context.notificationDataStore.edit {
            it[QUIET_START] = startHour
            it[QUIET_END] = endHour
        }
    }

    suspend fun markDailyShown(timestamp: Long) {
        context.notificationDataStore.edit { it[LAST_DAILY] = timestamp }
    }

    suspend fun clear() {
        context.notificationDataStore.edit { it.clear() }
    }

    private companion object {
        val DAILY_ENABLED = booleanPreferencesKey("daily_check_in_enabled")
        val QUIET_START = intPreferencesKey("quiet_start_hour")
        val QUIET_END = intPreferencesKey("quiet_end_hour")
        val LAST_DAILY = longPreferencesKey("last_daily_shown_at")
    }
}
