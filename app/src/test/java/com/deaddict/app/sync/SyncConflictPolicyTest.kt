package com.deaddict.app.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncConflictPolicyTest {
    @Test
    fun identicalVersionsDoNotCreateConflict() {
        val version = SyncVersion(100L, "device-a", 2L)
        assertEquals(ConflictWinner.IDENTICAL, SyncConflictPolicy.decide(version, version).winner)
    }

    @Test
    fun higherRevisionWinsBeforeTimestamp() {
        val local = SyncVersion(200L, "device-a", 3L)
        val remote = SyncVersion(500L, "device-b", 2L)
        assertEquals(ConflictWinner.LOCAL, SyncConflictPolicy.decide(local, remote).winner)
    }

    @Test
    fun newerTimestampWinsWhenRevisionMatches() {
        val local = SyncVersion(200L, "device-a", 3L)
        val remote = SyncVersion(500L, "device-b", 3L)
        assertEquals(ConflictWinner.REMOTE, SyncConflictPolicy.decide(local, remote).winner)
    }

    @Test
    fun deviceIdProvidesStableTieBreak() {
        val local = SyncVersion(500L, "device-z", 3L)
        val remote = SyncVersion(500L, "device-a", 3L)
        assertEquals(ConflictWinner.LOCAL, SyncConflictPolicy.decide(local, remote).winner)
        assertEquals(ConflictWinner.REMOTE, SyncConflictPolicy.decide(remote, local).winner)
    }

    @Test
    fun firstAccountInitializesWithoutClearing() {
        assertEquals(
            AccountScopeAction.INITIALIZE_SCOPE,
            AccountIsolationPolicy.action(null, "user-a"),
        )
        assertEquals(AccountScope("user-a", 0L), AccountIsolationPolicy.nextScope(null, "user-a"))
    }

    @Test
    fun sameAccountKeepsLocalDataAndGeneration() {
        val stored = AccountScope("user-a", 4L)
        assertEquals(
            AccountScopeAction.KEEP_LOCAL_DATA,
            AccountIsolationPolicy.action(stored, "user-a"),
        )
        assertEquals(stored, AccountIsolationPolicy.nextScope(stored, "user-a"))
    }

    @Test
    fun changedAccountRequiresClearAndGenerationBump() {
        val stored = AccountScope("user-a", 4L)
        assertEquals(
            AccountScopeAction.CLEAR_LOCAL_DATA,
            AccountIsolationPolicy.action(stored, "user-b"),
        )
        assertEquals(AccountScope("user-b", 5L), AccountIsolationPolicy.nextScope(stored, "user-b"))
    }
}
