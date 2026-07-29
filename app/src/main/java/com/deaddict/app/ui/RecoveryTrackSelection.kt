package com.deaddict.app.ui

import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus

data class RecoveryTrackSelectionCandidate(
    val id: String,
    val role: RecoveryTrackRole,
    val status: RecoveryTrackStatus,
)

fun resolveSelectedRecoveryTrackId(
    tracks: List<RecoveryTrackSelectionCandidate>,
    requestedId: String?,
): String? {
    tracks.firstOrNull { it.id == requestedId }?.let { return it.id }
    tracks.firstOrNull {
        it.role == RecoveryTrackRole.PRIMARY &&
            it.status in setOf(RecoveryTrackStatus.ACTIVE, RecoveryTrackStatus.MAINTENANCE)
    }?.let { return it.id }
    tracks.firstOrNull { it.status == RecoveryTrackStatus.ACTIVE }?.let { return it.id }
    tracks.firstOrNull { it.status == RecoveryTrackStatus.MAINTENANCE }?.let { return it.id }
    return tracks.firstOrNull()?.id
}
