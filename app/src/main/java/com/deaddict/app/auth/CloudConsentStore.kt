package com.deaddict.app.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.deaddict.database.repository.GuestMigrationRepository
import com.deaddict.database.repository.GuestMigrationResult
import com.deaddict.database.repository.GuestUploadConsent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cloudConsentDataStore by preferencesDataStore("cloud_consent")

class CloudConsentStore(
    private val context: Context,
    private val migrationRepository: GuestMigrationRepository,
) {
    val guestUploadAccepted: Flow<Boolean> =
        context.cloudConsentDataStore.data.map { it[GUEST_UPLOAD_ACCEPTED] ?: false }

    suspend fun acceptAndQueueGuestData(): GuestMigrationResult {
        val result = migrationRepository.queueExistingGuestData(
            GuestUploadConsent.explicitlyAccepted(),
        )
        context.cloudConsentDataStore.edit { it[GUEST_UPLOAD_ACCEPTED] = true }
        return result
    }

    companion object {
        private val GUEST_UPLOAD_ACCEPTED = booleanPreferencesKey("guest_upload_accepted")
    }
}

