package com.deaddict.app.ui

import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecoveryTrackSelectionTest {
    @Test
    fun `requested track wins without changing primary role`() {
        val tracks = listOf(
            candidate("primary", RecoveryTrackRole.PRIMARY),
            candidate("supporting", RecoveryTrackRole.SUPPORTING),
        )

        assertEquals(
            "supporting",
            resolveSelectedRecoveryTrackId(tracks, "supporting"),
        )
    }

    @Test
    fun `missing selection falls back to current primary`() {
        val tracks = listOf(
            candidate("supporting", RecoveryTrackRole.SUPPORTING),
            candidate("primary", RecoveryTrackRole.PRIMARY),
        )

        assertEquals(
            "primary",
            resolveSelectedRecoveryTrackId(tracks, "missing"),
        )
    }

    @Test
    fun `paused supporting track remains explicitly selectable`() {
        val tracks = listOf(
            candidate("primary", RecoveryTrackRole.PRIMARY),
            candidate(
                id = "paused",
                role = RecoveryTrackRole.SUPPORTING,
                status = RecoveryTrackStatus.PAUSED,
            ),
        )

        assertEquals(
            "paused",
            resolveSelectedRecoveryTrackId(tracks, "paused"),
        )
    }

    @Test
    fun `empty tracks have no selection`() {
        assertNull(resolveSelectedRecoveryTrackId(emptyList(), null))
    }

    private fun candidate(
        id: String,
        role: RecoveryTrackRole,
        status: RecoveryTrackStatus = RecoveryTrackStatus.ACTIVE,
    ) = RecoveryTrackSelectionCandidate(id, role, status)
}
