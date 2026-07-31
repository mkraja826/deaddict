package com.deaddict.app.coach

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    val tone: RookTone = RookTone.DIRECT,
    val avatarVisible: Boolean = true,
    val trackTones: Map<String, RookTone> = emptyMap(),
) {
    fun toneFor(trackId: String?): RookTone =
        trackId?.let(trackTones::get) ?: tone

    fun hasTrackOverride(trackId: String?): Boolean =
        trackId != null && trackId in trackTones
}

internal object RookTrackToneCodec {
    private const val SEPARATOR = "="

    fun encode(values: Map<String, RookTone>): Set<String> =
        values.entries.mapTo(linkedSetOf()) { (trackId, tone) ->
            "$trackId$SEPARATOR${tone.name}"
        }

    fun decode(values: Set<String>?): Map<String, RookTone> =
        values.orEmpty().mapNotNull { encoded ->
            val separatorIndex = encoded.lastIndexOf(SEPARATOR)
            if (separatorIndex <= 0 || separatorIndex == encoded.lastIndex) return@mapNotNull null
            val trackId = encoded.substring(0, separatorIndex)
            val tone = runCatching {
                RookTone.valueOf(encoded.substring(separatorIndex + SEPARATOR.length))
            }.getOrNull() ?: return@mapNotNull null
            trackId to tone
        }.toMap()
}

class RookPreferenceStore(private val context: Context) {
    val preferences: Flow<RookPreferences> = context.rookPreferenceDataStore.data.map { values ->
        RookPreferences(
            enabled = values[ENABLED] ?: true,
            tone = values[TONE]
                ?.let { stored -> runCatching { RookTone.valueOf(stored) }.getOrNull() }
                ?: RookTone.DIRECT,
            avatarVisible = values[AVATAR_VISIBLE] ?: true,
            trackTones = RookTrackToneCodec.decode(values[TRACK_TONES]),
        )
    }

    suspend fun setTone(tone: RookTone) {
        context.rookPreferenceDataStore.edit { it[TONE] = tone.name }
    }

    suspend fun setTrackTone(trackId: String, tone: RookTone?) {
        require(trackId.isNotBlank()) { "Recovery Track ID is required" }
        context.rookPreferenceDataStore.edit { values ->
            val next = RookTrackToneCodec.decode(values[TRACK_TONES]).toMutableMap()
            if (tone == null) next.remove(trackId) else next[trackId] = tone
            values[TRACK_TONES] = RookTrackToneCodec.encode(next)
        }
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
        val TRACK_TONES = stringSetPreferencesKey("rook_track_tones")
    }
}
