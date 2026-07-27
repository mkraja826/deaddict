package com.deaddict.app.export

/** Lightweight gate applied before any imported backup can reach persistence. */
data class PortableImportMetadata(
    val schemaVersion: Int,
    val exportedAtEpochMillis: Long,
    val programCount: Int,
    val trackingEventCount: Int,
    val rescueSessionCount: Int,
    val byteSize: Long,
)

enum class ImportRejectionReason {
    UNSUPPORTED_SCHEMA,
    INVALID_TIMESTAMP,
    NEGATIVE_COUNT,
    TOO_MANY_RECORDS,
    FILE_TOO_LARGE,
}

sealed interface ImportValidationResult {
    data object Accepted : ImportValidationResult
    data class Rejected(val reason: ImportRejectionReason) : ImportValidationResult
}

object PortableImportValidator {
    const val CURRENT_SCHEMA_VERSION = 1
    const val MAX_PROGRAMS = 500
    const val MAX_TRACKING_EVENTS = 250_000
    const val MAX_RESCUE_SESSIONS = 50_000
    const val MAX_FILE_BYTES = 100L * 1024L * 1024L

    fun validate(metadata: PortableImportMetadata): ImportValidationResult {
        if (metadata.schemaVersion != CURRENT_SCHEMA_VERSION) {
            return ImportValidationResult.Rejected(ImportRejectionReason.UNSUPPORTED_SCHEMA)
        }
        if (metadata.exportedAtEpochMillis <= 0L) {
            return ImportValidationResult.Rejected(ImportRejectionReason.INVALID_TIMESTAMP)
        }
        if (metadata.programCount < 0 || metadata.trackingEventCount < 0 || metadata.rescueSessionCount < 0) {
            return ImportValidationResult.Rejected(ImportRejectionReason.NEGATIVE_COUNT)
        }
        if (
            metadata.programCount > MAX_PROGRAMS ||
            metadata.trackingEventCount > MAX_TRACKING_EVENTS ||
            metadata.rescueSessionCount > MAX_RESCUE_SESSIONS
        ) {
            return ImportValidationResult.Rejected(ImportRejectionReason.TOO_MANY_RECORDS)
        }
        if (metadata.byteSize !in 1..MAX_FILE_BYTES) {
            return ImportValidationResult.Rejected(ImportRejectionReason.FILE_TOO_LARGE)
        }
        return ImportValidationResult.Accepted
    }
}
