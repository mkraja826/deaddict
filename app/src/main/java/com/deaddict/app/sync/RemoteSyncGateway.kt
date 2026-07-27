package com.deaddict.app.sync

import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.TrackingEventEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface RemoteSyncGateway {
    val available: Boolean

    suspend fun currentUserId(): String?

    suspend fun upsertProgram(userId: String, program: ActiveProgramEntity)

    suspend fun upsertTrackingEvent(userId: String, event: TrackingEventEntity)

    suspend fun upsertRescueSession(userId: String, session: RescueSessionEntity)
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
)

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
)

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
)

private fun Long.toIsoInstant(): String = Instant.ofEpochMilli(this).toString()
