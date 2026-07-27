package com.deaddict.app.export

import org.junit.Assert.assertEquals
import org.junit.Test

class PortableImportValidatorTest {
    private fun valid() = PortableImportMetadata(
        schemaVersion = 1,
        exportedAtEpochMillis = 1_700_000_000_000,
        programCount = 2,
        trackingEventCount = 100,
        rescueSessionCount = 5,
        byteSize = 4096,
    )

    @Test fun acceptsValidMetadata() {
        assertEquals(ImportValidationResult.Accepted, PortableImportValidator.validate(valid()))
    }

    @Test fun rejectsUnsupportedSchema() {
        assertEquals(
            ImportValidationResult.Rejected(ImportRejectionReason.UNSUPPORTED_SCHEMA),
            PortableImportValidator.validate(valid().copy(schemaVersion = 2)),
        )
    }

    @Test fun rejectsInvalidTimestampAndNegativeCounts() {
        assertEquals(
            ImportValidationResult.Rejected(ImportRejectionReason.INVALID_TIMESTAMP),
            PortableImportValidator.validate(valid().copy(exportedAtEpochMillis = 0)),
        )
        assertEquals(
            ImportValidationResult.Rejected(ImportRejectionReason.NEGATIVE_COUNT),
            PortableImportValidator.validate(valid().copy(trackingEventCount = -1)),
        )
    }

    @Test fun rejectsOversizedImports() {
        assertEquals(
            ImportValidationResult.Rejected(ImportRejectionReason.TOO_MANY_RECORDS),
            PortableImportValidator.validate(
                valid().copy(trackingEventCount = PortableImportValidator.MAX_TRACKING_EVENTS + 1),
            ),
        )
        assertEquals(
            ImportValidationResult.Rejected(ImportRejectionReason.FILE_TOO_LARGE),
            PortableImportValidator.validate(
                valid().copy(byteSize = PortableImportValidator.MAX_FILE_BYTES + 1),
            ),
        )
    }
}
