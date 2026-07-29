package com.deaddict.model

import com.deaddict.programs.ProgramId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryTrackTest {
    private val createdAt = Instant.parse("2026-07-29T12:00:00Z")

    @Test
    fun `new active primary track is valid`() {
        val track = activeTrack()

        assertEquals(RecoveryTrackRole.PRIMARY, track.role)
        assertEquals(RecoveryTrackStatus.ACTIVE, track.status)
        assertTrue(track.isOpen)
    }

    @Test
    fun `pausing a primary track demotes it and increments revision`() {
        val pausedAt = createdAt.plusSeconds(60)

        val paused = activeTrack().transitionTo(RecoveryTrackStatus.PAUSED, pausedAt)

        assertEquals(RecoveryTrackRole.SUPPORTING, paused.role)
        assertEquals(RecoveryTrackStatus.PAUSED, paused.status)
        assertEquals(pausedAt, paused.pausedAt)
        assertEquals(1L, paused.revision)
    }

    @Test
    fun `paused track can resume as supporting`() {
        val pausedAt = createdAt.plusSeconds(60)
        val resumedAt = pausedAt.plusSeconds(60)
        val paused = activeTrack().transitionTo(RecoveryTrackStatus.PAUSED, pausedAt)

        val resumed = paused.transitionTo(RecoveryTrackStatus.ACTIVE, resumedAt)

        assertEquals(RecoveryTrackRole.SUPPORTING, resumed.role)
        assertEquals(RecoveryTrackStatus.ACTIVE, resumed.status)
        assertNull(resumed.pausedAt)
        assertEquals(2L, resumed.revision)
    }

    @Test
    fun `archived track cannot transition`() {
        val archived = activeTrack().transitionTo(
            RecoveryTrackStatus.ARCHIVED,
            createdAt.plusSeconds(60),
        )

        assertFalse(archived.isOpen)
        assertFalse(archived.canTransitionTo(RecoveryTrackStatus.ACTIVE))
        assertFails { archived.transitionTo(RecoveryTrackStatus.ACTIVE, createdAt.plusSeconds(120)) }
    }

    @Test
    fun `primary paused track cannot be constructed`() {
        assertFails {
            activeTrack().copy(
                status = RecoveryTrackStatus.PAUSED,
                pausedAt = createdAt.plusSeconds(60),
            )
        }
    }

    @Test
    fun `owner key parser rejects an empty scoped value`() {
        assertFails { OwnerKey.parse("guest:") }
        assertFails { OwnerKey.parse("user:") }
    }

    @Test
    fun `track id rejects non uuid input`() {
        assertFails { RecoveryTrackId.parse("not-a-uuid") }
    }

    private fun activeTrack(): RecoveryTrack = RecoveryTrack(
        id = RecoveryTrackId.parse("7ebdbd0b-4676-45f1-82cd-e632b3ec6092"),
        ownerKey = OwnerKey.guest("f414ce7d-4d6e-463d-b48a-41835e03812b"),
        programId = ProgramId("smoking"),
        displayAlias = null,
        role = RecoveryTrackRole.PRIMARY,
        status = RecoveryTrackStatus.ACTIVE,
        startedAt = createdAt,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun assertFails(block: () -> Unit) {
        assertTrue(runCatching(block).isFailure)
    }
}
