package com.deaddict.app.privacy

import com.deaddict.app.auth.SupabaseClientProvider
import com.deaddict.app.notifications.NotificationScheduler
import com.deaddict.app.sync.SyncScheduler
import com.deaddict.database.DeAddictDatabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

interface AccountDeletionGateway {
    val available: Boolean

    suspend fun deleteRemoteAccount()
}

data class AccountDeletionResult(
    val localCleanupComplete: Boolean,
)

@Singleton
class SupabaseAccountDeletionGateway @Inject constructor(
    provider: SupabaseClientProvider,
) : AccountDeletionGateway {
    private val client: SupabaseClient? = provider.client

    override val available: Boolean = client != null

    override suspend fun deleteRemoteAccount() {
        val supabase = checkNotNull(client) { "Supabase is not configured" }
        checkNotNull(supabase.auth.currentUserOrNull()) { "No authenticated account to delete" }

        val response = supabase.functions.invoke("delete-account")
        check(response.status.value in 200..299) {
            "Account deletion service returned HTTP ${response.status.value}"
        }
        supabase.auth.clearSession()
    }
}

@Singleton
class AccountDeletionCoordinator @Inject constructor(
    private val remote: SupabaseAccountDeletionGateway,
    private val database: DeAddictDatabase,
    private val privacyPreferences: PrivacyPreferenceStore,
    private val notificationScheduler: NotificationScheduler,
    private val syncScheduler: SyncScheduler,
) {
    val available: Boolean
        get() = remote.available

    suspend fun deleteAccount(): AccountDeletionResult {
        remote.deleteRemoteAccount()
        val cleanupResults = withContext(NonCancellable + Dispatchers.IO) {
            listOf(
                runCatching { syncScheduler.cancelAll() },
                runCatching { notificationScheduler.cancelDailyCheckIn() },
                runCatching { database.clearAllTables() },
                runCatching { privacyPreferences.clear() },
            )
        }
        return AccountDeletionResult(
            localCleanupComplete = cleanupResults.all(Result<Unit>::isSuccess),
        )
    }
}
