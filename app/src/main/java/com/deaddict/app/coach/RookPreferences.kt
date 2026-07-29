package com.deaddict.app.coach

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rookPreferenceDataStore by preferencesDataStore("rook_preferences")

enum class RookTone {
    DIRECT,
    BRUTAL_BANTER,
    QUIET,
}

data class RookPreferences(
    val enabled: Boolean = true,
    val tone: RookTone = RookTone.BRUTAL_BANTER,
    val avatarVisible: Boolean = true,
)

class RookPreferenceStore(private val context: Context) {
    val preferences: Flow<RookPreferences> = context.rookPreferenceDataStore.data.map { values ->
        RookPreferences(
            enabled = values[ENABLED] ?: true,
            tone = values[TONE]
                ?.let { stored -> runCatching { RookTone.valueOf(stored) }.getOrNull() }
                ?: RookTone.BRUTAL_BANTER,
            avatarVisible = values[AVATAR_VISIBLE] ?: true,
        )
    }

    suspend fun setTone(tone: RookTone) {
        context.rookPreferenceDataStore.edit { it[TONE] = tone.name }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.rookPreferenceDataStore.edit { it[ENABLED] = enabled }
    }

    suspend fun setAvatarVisible(visible: Boolean) {
        context.rookPreferenceDataStore.edit { it[AVATAR_VISIBLE] = visible }
    }

    suspend fun clear() {
        context.rookPreferenceDataStore.edit { it.clear() }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("rook_enabled")
        val TONE = stringPreferencesKey("rook_tone")
        val AVATAR_VISIBLE = booleanPreferencesKey("rook_avatar_visible")
    }
}
