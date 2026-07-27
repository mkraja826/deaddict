package com.deaddict.app.sync

/** Metadata used to resolve the same logical record arriving from multiple devices. */
data class SyncVersion(
    val updatedAtEpochMillis: Long,
    val deviceId: String,
    val revision: Long,
) {
    init {
        require(updatedAtEpochMillis >= 0L)
        require(deviceId.isNotBlank())
        require(revision >= 0L)
    }
}

enum class ConflictWinner { LOCAL, REMOTE, IDENTICAL }

data class ConflictDecision(
    val winner: ConflictWinner,
    val reason: String,
)

/**
 * Deterministic last-write policy with stable tie breaking.
 *
 * Revision wins first because it captures explicit logical progress. Timestamp is next, and
 * device id is the final stable tie-breaker so every client reaches the same result.
 */
object SyncConflictPolicy {
    fun decide(local: SyncVersion, remote: SyncVersion): ConflictDecision {
        if (local == remote) return ConflictDecision(ConflictWinner.IDENTICAL, "same-version")

        return when {
            local.revision > remote.revision -> ConflictDecision(ConflictWinner.LOCAL, "higher-revision")
            remote.revision > local.revision -> ConflictDecision(ConflictWinner.REMOTE, "higher-revision")
            local.updatedAtEpochMillis > remote.updatedAtEpochMillis ->
                ConflictDecision(ConflictWinner.LOCAL, "newer-timestamp")
            remote.updatedAtEpochMillis > local.updatedAtEpochMillis ->
                ConflictDecision(ConflictWinner.REMOTE, "newer-timestamp")
            local.deviceId > remote.deviceId -> ConflictDecision(ConflictWinner.LOCAL, "device-tiebreak")
            else -> ConflictDecision(ConflictWinner.REMOTE, "device-tiebreak")
        }
    }
}

/** Guards local state against accidental reuse after signing into a different account. */
data class AccountScope(
    val userId: String,
    val generation: Long,
) {
    init {
        require(userId.isNotBlank())
        require(generation >= 0L)
    }
}

enum class AccountScopeAction { KEEP_LOCAL_DATA, CLEAR_LOCAL_DATA, INITIALIZE_SCOPE }

object AccountIsolationPolicy {
    fun action(stored: AccountScope?, currentUserId: String): AccountScopeAction {
        require(currentUserId.isNotBlank())
        return when {
            stored == null -> AccountScopeAction.INITIALIZE_SCOPE
            stored.userId == currentUserId -> AccountScopeAction.KEEP_LOCAL_DATA
            else -> AccountScopeAction.CLEAR_LOCAL_DATA
        }
    }

    fun nextScope(stored: AccountScope?, currentUserId: String): AccountScope {
        require(currentUserId.isNotBlank())
        val nextGeneration = when {
            stored == null -> 0L
            stored.userId == currentUserId -> stored.generation
            else -> stored.generation + 1L
        }
        return AccountScope(currentUserId, nextGeneration)
    }
}
