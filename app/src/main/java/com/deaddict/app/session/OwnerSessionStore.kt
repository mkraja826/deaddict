package com.deaddict.app.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.deaddict.app.auth.AuthAvailability
import com.deaddict.app.auth.AuthGateway
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryTrackId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ownerSessionDataStore by preferencesDataStore("owner_session")

data class OwnerSessionState(
    val ownerKey: OwnerKey? = null,
    val guestOwnerKey: OwnerKey? = null,
    val selectedRecoveryTrackId: RecoveryTrackId? = null,
)

class OwnerSessionStore(
    private val context: Context,
) {
    val state: Flow<OwnerSessionState> = context.ownerSessionDataStore.data.map { values ->
        val owner = values[CURRENT_OWNER]
            ?.let { stored -> runCatching { OwnerKey.parse(stored) }.getOrNull() }
        val guest = values[GUEST_PROFILE_ID]
            ?.let { profileId -> runCatching { OwnerKey.guest(profileId) }.getOrNull() }
        val selected = if (values[SELECTED_OWNER] == owner?.value) {
            values[SELECTED_TRACK]
                ?.let { stored -> runCatching { RecoveryTrackId.parse(stored) }.getOrNull() }
        } else {
            null
        }
        OwnerSessionState(
            ownerKey = owner,
            guestOwnerKey = guest,
            selectedRecoveryTrackId = selected,
        )
    }

    suspend fun resolve(authGateway: AuthGateway): OwnerKey {
        val authenticatedUser = when (authGateway.availability) {
            AuthAvailability.Configured -> runCatching { authGateway.currentUser() }.getOrNull()
            is AuthAvailability.Unavailable -> null
        }
        return authenticatedUser
            ?.let { establishAuthenticated(it.id) }
            ?: establishGuest()
    }

    suspend fun establishAuthenticated(userId: String): OwnerKey {
        val owner = OwnerKey.authenticated(userId)
        context.ownerSessionDataStore.edit { values ->
            ensureGuestProfile(values)
            switchOwner(values, owner)
        }
        return owner
    }

    suspend fun establishGuest(): OwnerKey {
        var owner: OwnerKey? = null
        context.ownerSessionDataStore.edit { values ->
            val profileId = ensureGuestProfile(values)
            owner = OwnerKey.guest(profileId)
            switchOwner(values, checkNotNull(owner))
        }
        return checkNotNull(owner)
    }

    suspend fun guestOwnerKey(): OwnerKey {
        var owner: OwnerKey? = null
        context.ownerSessionDataStore.edit { values ->
            owner = OwnerKey.guest(ensureGuestProfile(values))
        }
        return checkNotNull(owner)
    }

    suspend fun selectRecoveryTrack(
        ownerKey: OwnerKey,
        trackId: RecoveryTrackId?,
    ) {
        context.ownerSessionDataStore.edit { values ->
            if (values[CURRENT_OWNER] != ownerKey.value) return@edit
            if (trackId == null) {
                values.remove(SELECTED_OWNER)
                values.remove(SELECTED_TRACK)
            } else {
                values[SELECTED_OWNER] = ownerKey.value
                values[SELECTED_TRACK] = trackId.value
            }
        }
    }

    private fun ensureGuestProfile(
        values: androidx.datastore.preferences.core.MutablePreferences,
    ): String {
        val existing = values[GUEST_PROFILE_ID]
        if (existing != null) return existing
        return UUID.randomUUID().toString().also { values[GUEST_PROFILE_ID] = it }
    }

    private fun switchOwner(
        values: androidx.datastore.preferences.core.MutablePreferences,
        owner: OwnerKey,
    ) {
        if (values[CURRENT_OWNER] != owner.value) {
            values.remove(SELECTED_OWNER)
            values.remove(SELECTED_TRACK)
        }
        values[CURRENT_OWNER] = owner.value
    }

    private companion object {
        val CURRENT_OWNER = stringPreferencesKey("current_owner")
        val GUEST_PROFILE_ID = stringPreferencesKey("guest_profile_id")
        val SELECTED_OWNER = stringPreferencesKey("selected_track_owner")
        val SELECTED_TRACK = stringPreferencesKey("selected_track_id")
    }
}
