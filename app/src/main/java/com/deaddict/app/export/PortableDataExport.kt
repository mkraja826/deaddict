package com.deaddict.app.export

import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.TrackingEventEntity

/** Privacy-preserving, dependency-free export payload used by share and backup surfaces. */
data class PortableExportSnapshot(
    val exportedAtEpochMillis: Long,
    val programs: List<ActiveProgramEntity>,
    val trackingEvents: List<TrackingEventEntity>,
    val rescueSessions: List<RescueSessionEntity>,
)

object PortableDataExporter {
    fun toJson(snapshot: PortableExportSnapshot): String = buildString {
        append('{')
        append("\"schemaVersion\":1,")
        append("\"exportedAtEpochMillis\":${snapshot.exportedAtEpochMillis},")
        append("\"programs\":[")
        append(snapshot.programs.joinToString(",") { program ->
            "{\"id\":${json(program.id)},\"programId\":${json(program.programId)}," +
                "\"activatedAtEpochMillis\":${program.activatedAtEpochMillis}," +
                "\"archivedAtEpochMillis\":${program.archivedAtEpochMillis ?: "null"}}"
        })
        append("],\"trackingEvents\":[")
        append(snapshot.trackingEvents.joinToString(",") { event ->
            "{\"id\":${json(event.id)},\"programId\":${json(event.programId)}," +
                "\"kind\":${json(event.kind.name)},\"quantity\":${event.quantity ?: "null"}," +
                "\"unit\":${jsonOrNull(event.unit)},\"costMinorUnits\":${event.costMinorUnits ?: "null"}," +
                "\"urgeIntensity\":${event.urgeIntensity ?: "null"},\"triggerKey\":${jsonOrNull(event.triggerKey)}," +
                "\"occurredAtEpochMillis\":${event.occurredAtEpochMillis}," +
                "\"createdAtEpochMillis\":${event.createdAtEpochMillis},\"privateNote\":${jsonOrNull(event.privateNote)}}"
        })
        append("],\"rescueSessions\":[")
        append(snapshot.rescueSessions.joinToString(",") { session ->
            "{\"id\":${json(session.id)},\"programId\":${json(session.programId)}," +
                "\"startedAtEpochMillis\":${session.startedAtEpochMillis}," +
                "\"completedAtEpochMillis\":${session.completedAtEpochMillis ?: "null"}," +
                "\"initialUrge\":${session.initialUrge},\"finalUrge\":${session.finalUrge ?: "null"}," +
                "\"triggerKey\":${jsonOrNull(session.triggerKey)}," +
                "\"actionKeys\":[${session.actionKeys.joinToString(",") { json(it) }}]," +
                "\"outcome\":${jsonOrNull(session.outcome?.name)}}"
        })
        append("]}")
    }

    fun trackingEventsCsv(events: List<TrackingEventEntity>): String = buildString {
        appendLine("id,program_id,kind,quantity,unit,cost_minor_units,urge_intensity,trigger,occurred_at_epoch_ms,created_at_epoch_ms,private_note")
        events.forEach { event ->
            appendLine(
                listOf(
                    event.id,
                    event.programId,
                    event.kind.name,
                    event.quantity,
                    event.unit,
                    event.costMinorUnits,
                    event.urgeIntensity,
                    event.triggerKey,
                    event.occurredAtEpochMillis,
                    event.createdAtEpochMillis,
                    event.privateNote,
                ).joinToString(",") { csv(it?.toString().orEmpty()) },
            )
        }
    }

    private fun jsonOrNull(value: String?): String = value?.let(::json) ?: "null"

    private fun json(value: String): String = buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) append("\\u%04x".format(char.code)) else append(char)
            }
        }
        append('"')
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
