package com.deaddict.app.insights

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryTrackId
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.insightPreferenceDataStore by preferencesDataStore("insight_preferences")

data class InsightControls(
    val window: InsightWindow = InsightWindow.SEVEN_DAYS,
    val hiddenOtherTrackIds: Set<String> = emptySet(),
)

class InsightPreferenceStore(private val context: Context) {
    fun observe(
        ownerKey: OwnerKey,
        selectedTrackId: RecoveryTrackId,
    ): Flow<InsightControls> {
        val windowKey = windowKey(ownerKey)
        val hiddenKey = hiddenKey(ownerKey, selectedTrackId)
        return context.insightPreferenceDataStore.data.map { values ->
            InsightControls(
                window = values[windowKey]
                    ?.let { stored -> InsightWindow.entries.firstOrNull { it.name == stored } }
                    ?: InsightWindow.SEVEN_DAYS,
                hiddenOtherTrackIds = values[hiddenKey].orEmpty(),
            )
        }
    }

    suspend fun setWindow(ownerKey: OwnerKey, window: InsightWindow) {
        context.insightPreferenceDataStore.edit { values ->
            values[windowKey(ownerKey)] = window.name
        }
    }

    suspend fun hideComparison(
        ownerKey: OwnerKey,
        selectedTrackId: RecoveryTrackId,
        otherTrackId: RecoveryTrackId,
    ) {
        require(selectedTrackId != otherTrackId) { "A Recovery Track cannot hide itself" }
        context.insightPreferenceDataStore.edit { values ->
            val key = hiddenKey(ownerKey, selectedTrackId)
            values[key] = values[key].orEmpty() + otherTrackId.value
        }
    }

    suspend fun restoreComparisons(
        ownerKey: OwnerKey,
        selectedTrackId: RecoveryTrackId,
    ) {
        context.insightPreferenceDataStore.edit { values ->
            values.remove(hiddenKey(ownerKey, selectedTrackId))
        }
    }

    suspend fun clear() {
        context.insightPreferenceDataStore.edit { it.clear() }
    }

    private fun windowKey(ownerKey: OwnerKey) =
        stringPreferencesKey("window_${scopeHash(ownerKey.value)}")

    private fun hiddenKey(ownerKey: OwnerKey, selectedTrackId: RecoveryTrackId) =
        stringSetPreferencesKey("hidden_${scopeHash("${ownerKey.value}|${selectedTrackId.value}")}")

    private fun scopeHash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
        .take(24)
}
