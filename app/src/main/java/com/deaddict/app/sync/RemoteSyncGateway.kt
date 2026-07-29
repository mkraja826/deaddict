package com.deaddict.app.sync

import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.TrackingEventEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class CloudSnapshot(
    val programs: List<RemoteProgramRecord>,
    val trackingEvents: List<RemoteTrackingRecord>,
    val rescueSessions: List<RemoteRescueRecord>,
    val recoveryTracks: List<RemoteRecoveryTrackRecord> = emptyList(),
    val recoveryGoals: List<RemoteRecoveryGoalRecord> = emptyList(),
)

data class RemoteProgramRecord(
    val id: String,
    val programId: String,
    val activatedAtEpochMillis: Long,
    val archivedAtEpochMillis: Long?,
    val clientUpdatedAtEpochMillis: Long,
)

data class RemoteRecoveryTrackRecord(
    val id: String,
    val userId: String,
    val programId: String,
    val displayAlias: String?,
    val role: String,
    val status: String,
    val startedAtEpochMillis: Long,
    val pausedAtEpochMillis: Long?,
    val maintenanceAtEpochMillis: Long?,
    val archivedAtEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val clientUpdatedAtEpochMillis: Long,
    val revision: Long,
)

data class RemoteRecoveryGoalRecord(
    val id: String,
    val userId: String,
    val recoveryTrackId: String,
    val goalType: String,
    val targetValue: Double?,
    val unitKey: String?,
    val periodType: String?,
    val title: String?,
    val effectiveFromEpochMillis: Long,
    val effectiveUntilEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val clientUpdatedAtEpochMillis: Long,
    val revision: Long,
)

data class RemoteTrackingRecord(
    val id: String,
    val programId: String,
    val kind: String,
    val quantity: Double?,
    val unit: String?,
    val costMinorUnits: Long?,
    val urgeIntensity: Int?,
    val triggerKey: String?,
    val occurredAtEpochMillis: Long,
    val clientUpdatedAtEpochMillis: Long,
)

data class RemoteRescueRecord(
    val id: String,
    val programId: String,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
    val initialUrge: Int,
    val finalUrge: Int?,
    val triggerKey: String?,
    val actionKeys: List<String>,
    val outcome: String?,
    val clientUpdatedAtEpochMillis: Long,
)

interface RemoteSyncGateway {
    val available: Boolean

    suspend fun currentUserId(): String?

    suspend fun upsertProgram(userId: String, program: ActiveProgramEntity)

    suspend fun upsertRecoveryTrack(userId: String, track: RecoveryTrackEntity)

    suspend fun upsertRecoveryGoal(userId: String, goal: RecoveryGoalVersionEntity)

    suspend fun upsertTrackingEvent(userId: String, event: TrackingEventEntity)

    suspend fun upsertRescueSession(userId: String, session: RescueSessionEntity)

    suspend fun deleteRecord(userId: String, aggregateType: SyncAggregateType, aggregateId: String)

    suspend fun downloadSnapshot(): CloudSnapshot
}

class SupabaseRemoteSyncGateway(
    private val client: SupabaseClient?,
) : RemoteSyncGateway {
    override val available: Boolean = client != null

    override suspend fun currentUserId(): String? =
        client?.auth?.currentUserOrNull()?.id

    override suspend fun upsertProgram(userId: String, program: ActiveProgramEntity) {
        requireClient().from("user_programs").upsert(
            CloudProgram(
                id = program.id,
                userId = userId,
                programId = program.programId,
                activatedAt = program.activatedAtEpochMillis.toIsoInstant(),
                archivedAt = program.archivedAtEpochMillis?.toIsoInstant(),
                clientUpdatedAt = maxOf(
                    program.activatedAtEpochMillis,
                    program.archivedAtEpochMillis ?: program.activatedAtEpochMillis,
                ).toIsoInstant(),
            ),
        )
    }

    override suspend fun upsertRecoveryTrack(userId: String, track: RecoveryTrackEntity) {
        requireClient().from("recovery_tracks").upsert(
            CloudRecoveryTrack(
                id = track.id,
                userId = userId,
                programId = track.programId,
                displayAlias = track.displayAlias,
                role = track.role.name,
                status = track.status.name,
                startedAt = track.startedAtEpochMillis.toIsoInstant(),
                pausedAt = track.pausedAtEpochMillis?.toIsoInstant(),
                maintenanceAt = track.maintenanceAtEpochMillis?.toIsoInstant(),
                archivedAt = track.archivedAtEpochMillis?.toIsoInstant(),
                clientUpdatedAt = track.updatedAtEpochMillis.toIsoInstant(),
                revision = track.revision,
                createdAt = track.createdAtEpochMillis.toIsoInstant(),
            ),
        )
    }

    override suspend fun upsertRecoveryGoal(userId: String, goal: RecoveryGoalVersionEntity) {
        requireClient().from("recovery_goal_versions").upsert(
            CloudRecoveryGoal(
                id = goal.id,
                userId = userId,
                recoveryTrackId = goal.recoveryTrackId,
                goalType = goal.goalType.name,
                targetValue = goal.targetValue,
                unitKey = goal.unitKey,
                periodType = goal.periodType?.name,
                title = goal.title,
                effectiveFrom = goal.effectiveFromEpochMillis.toIsoInstant(),
                effectiveUntil = goal.effectiveUntilEpochMillis?.toIsoInstant(),
                clientUpdatedAt = goal.updatedAtEpochMillis.toIsoInstant(),
                revision = goal.revision,
                createdAt = goal.createdAtEpochMillis.toIsoInstant(),
            ),
        )
    }

    override suspend fun upsertTrackingEvent(userId: String, event: TrackingEventEntity) {
        requireClient().from("tracking_events").upsert(
            CloudTrackingEvent(
                id = event.id,
                userId = userId,
                programId = event.programId,
                kind = event.kind.name,
                quantity = event.quantity,
                unit = event.unit,
                costMinorUnits = event.costMinorUnits,
                urgeIntensity = event.urgeIntensity,
                triggerKey = event.triggerKey,
                occurredAt = event.occurredAtEpochMillis.toIsoInstant(),
                clientUpdatedAt = event.createdAtEpochMillis.toIsoInstant(),
            ),
        )
    }

    override suspend fun upsertRescueSession(userId: String, session: RescueSessionEntity) {
        requireClient().from("rescue_sessions").upsert(
            CloudRescueSession(
                id = session.id,
                userId = userId,
                programId = session.programId,
                startedAt = session.startedAtEpochMillis.toIsoInstant(),
                completedAt = session.completedAtEpochMillis?.toIsoInstant(),
                initialUrge = session.initialUrge,
                finalUrge = session.finalUrge,
                triggerKey = session.triggerKey,
                actionKeys = session.actionKeys,
                outcome = session.outcome?.name,
                clientUpdatedAt = maxOf(
                    session.startedAtEpochMillis,
                    session.completedAtEpochMillis ?: session.startedAtEpochMillis,
                ).toIsoInstant(),
            ),
        )
    }

    override suspend fun deleteRecord(
        userId: String,
        aggregateType: SyncAggregateType,
        aggregateId: String,
    ) {
        val table = when (aggregateType) {
            SyncAggregateType.ACTIVE_PROGRAM -> "user_programs"
            SyncAggregateType.RECOVERY_TRACK -> "recovery_tracks"
            SyncAggregateType.RECOVERY_GOAL -> "recovery_goal_versions"
            SyncAggregateType.TRACKING_EVENT -> "tracking_events"
            SyncAggregateType.RESCUE_SESSION -> "rescue_sessions"
        }
        requireClient().from(table).delete {
            filter {
                eq("id", aggregateId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun downloadSnapshot(): CloudSnapshot {
        val supabase = requireClient()
        return CloudSnapshot(
            programs = supabase.from("user_programs")
                .select()
                .decodeList<CloudProgram>()
                .map(CloudProgram::toRemoteRecord),
            trackingEvents = supabase.from("tracking_events")
                .select()
                .decodeList<CloudTrackingEvent>()
                .map(CloudTrackingEvent::toRemoteRecord),
            rescueSessions = supabase.from("rescue_sessions")
                .select()
                .decodeList<CloudRescueSession>()
                .map(CloudRescueSession::toRemoteRecord),
            recoveryTracks = supabase.from("recovery_tracks")
                .select()
                .decodeList<CloudRecoveryTrack>()
                .map(CloudRecoveryTrack::toRemoteRecord),
            recoveryGoals = supabase.from("recovery_goal_versions")
                .select()
                .decodeList<CloudRecoveryGoal>()
                .map(CloudRecoveryGoal::toRemoteRecord),
        )
    }

    private fun requireClient(): SupabaseClient =
        checkNotNull(client) { "Supabase is not configured" }
}

@Serializable
private data class CloudProgram(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("program_id") val programId: String,
    @SerialName("activated_at") val activatedAt: String,
    @SerialName("archived_at") val archivedAt: String?,
    @SerialName("client_updated_at") val clientUpdatedAt: String,
) {
    fun toRemoteRecord() = RemoteProgramRecord(
        id = id,
        programId = programId,
        activatedAtEpochMillis = activatedAt.toEpochMillis(),
        archivedAtEpochMillis = archivedAt?.toEpochMillis(),
        clientUpdatedAtEpochMillis = clientUpdatedAt.toEpochMillis(),
    )
}

@Serializable
private data class CloudRecoveryTrack(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("program_id") val programId: String,
    @SerialName("display_alias") val displayAlias: String?,
    val role: String,
    val status: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("paused_at") val pausedAt: String?,
    @SerialName("maintenance_at") val maintenanceAt: String?,
    @SerialName("archived_at") val archivedAt: String?,
    @SerialName("client_updated_at") val clientUpdatedAt: String,
    val revision: Long,
    @SerialName("created_at") val createdAt: String,
) {
    fun toRemoteRecord() = RemoteRecoveryTrackRecord(
        id = id,
        userId = userId,
        programId = programId,
        displayAlias = displayAlias,
        role = role,
        status = status,
        startedAtEpochMillis = startedAt.toEpochMillis(),
        pausedAtEpochMillis = pausedAt?.toEpochMillis(),
        maintenanceAtEpochMillis = maintenanceAt?.toEpochMillis(),
        archivedAtEpochMillis = archivedAt?.toEpochMillis(),
        createdAtEpochMillis = createdAt.toEpochMillis(),
        clientUpdatedAtEpochMillis = clientUpdatedAt.toEpochMillis(),
        revision = revision,
    )
}

@Serializable
private data class CloudRecoveryGoal(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("recovery_track_id") val recoveryTrackId: String,
    @SerialName("goal_type") val goalType: String,
    @SerialName("target_value") val targetValue: Double?,
    @SerialName("unit_key") val unitKey: String?,
    @SerialName("period_type") val periodType: String?,
    val title: String?,
    @SerialName("effective_from") val effectiveFrom: String,
    @SerialName("effective_until") val effectiveUntil: String?,
    @SerialName("client_updated_at") val clientUpdatedAt: String,
    val revision: Long,
    @SerialName("created_at") val createdAt: String,
) {
    fun toRemoteRecord() = RemoteRecoveryGoalRecord(
        id = id,
        userId = userId,
        recoveryTrackId = recoveryTrackId,
        goalType = goalType,
        targetValue = targetValue,
        unitKey = unitKey,
        periodType = periodType,
        title = title,
        effectiveFromEpochMillis = effectiveFrom.toEpochMillis(),
        effectiveUntilEpochMillis = effectiveUntil?.toEpochMillis(),
        createdAtEpochMillis = createdAt.toEpochMillis(),
        clientUpdatedAtEpochMillis = clientUpdatedAt.toEpochMillis(),
        revision = revision,
    )
}

@Serializable
private data class CloudTrackingEvent(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("program_id") val programId: String,
    val kind: String,
    val quantity: Double?,
    val unit: String?,
    @SerialName("cost_minor_units") val costMinorUnits: Long?,
    @SerialName("urge_intensity") val urgeIntensity: Int?,
    @SerialName("trigger_key") val triggerKey: String?,
    @SerialName("occurred_at") val occurredAt: String,
    @SerialName("client_updated_at") val clientUpdatedAt: String,
) {
    fun toRemoteRecord() = RemoteTrackingRecord(
        id = id,
        programId = programId,
        kind = kind,
        quantity = quantity,
        unit = unit,
        costMinorUnits = costMinorUnits,
        urgeIntensity = urgeIntensity,
        triggerKey = triggerKey,
        occurredAtEpochMillis = occurredAt.toEpochMillis(),
        clientUpdatedAtEpochMillis = clientUpdatedAt.toEpochMillis(),
    )
}

@Serializable
private data class CloudRescueSession(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("program_id") val programId: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("completed_at") val completedAt: String?,
    @SerialName("initial_urge") val initialUrge: Int,
    @SerialName("final_urge") val finalUrge: Int?,
    @SerialName("trigger_key") val triggerKey: String?,
    @SerialName("action_keys") val actionKeys: List<String>,
    val outcome: String?,
    @SerialName("client_updated_at") val clientUpdatedAt: String,
) {
    fun toRemoteRecord() = RemoteRescueRecord(
        id = id,
        programId = programId,
        startedAtEpochMillis = startedAt.toEpochMillis(),
        completedAtEpochMillis = completedAt?.toEpochMillis(),
        initialUrge = initialUrge,
        finalUrge = finalUrge,
        triggerKey = triggerKey,
        actionKeys = actionKeys,
        outcome = outcome,
        clientUpdatedAtEpochMillis = clientUpdatedAt.toEpochMillis(),
    )
}

private fun Long.toIsoInstant(): String = Instant.ofEpochMilli(this).toString()

private fun String.toEpochMillis(): Long = Instant.parse(this).toEpochMilli()
