package com.deaddict.model

import com.deaddict.programs.ProgramId
import java.time.Instant
import java.util.UUID

@JvmInline
value class RecoveryTrackId private constructor(val value: String) {
    companion object {
        fun random(): RecoveryTrackId = RecoveryTrackId(UUID.randomUUID().toString())

        fun parse(value: String): RecoveryTrackId {
            UUID.fromString(value)
            return RecoveryTrackId(value)
        }
    }
}

@JvmInline
value class OwnerKey private constructor(val value: String) {
    val isGuest: Boolean get() = value.startsWith(GUEST_PREFIX)
    val isAuthenticated: Boolean get() = value.startsWith(USER_PREFIX)

    companion object {
        private const val GUEST_PREFIX = "guest:"
        private const val USER_PREFIX = "user:"
        const val LEGACY_LOCAL = "legacy-local"

        fun guest(profileId: String): OwnerKey {
            require(profileId.isNotBlank())
            return OwnerKey("$GUEST_PREFIX$profileId")
        }

        fun authenticated(userId: String): OwnerKey {
            require(userId.isNotBlank())
            return OwnerKey("$USER_PREFIX$userId")
        }

        fun legacyLocal(): OwnerKey = OwnerKey(LEGACY_LOCAL)

        fun parse(value: String): OwnerKey = when {
            value == LEGACY_LOCAL -> legacyLocal()
            value.startsWith(GUEST_PREFIX) && value.removePrefix(GUEST_PREFIX).isNotBlank() ->
                guest(value.removePrefix(GUEST_PREFIX))
            value.startsWith(USER_PREFIX) && value.removePrefix(USER_PREFIX).isNotBlank() ->
                authenticated(value.removePrefix(USER_PREFIX))
            else -> throw IllegalArgumentException(
                "Owner key must use guest:, user:, or legacy-local scope",
            )
        }
    }
}

enum class RecoveryTrackRole {
    PRIMARY,
    SUPPORTING,
}

enum class RecoveryTrackStatus {
    ACTIVE,
    PAUSED,
    MAINTENANCE,
    ARCHIVED,
}

data class RecoveryTrack(
    val id: RecoveryTrackId,
    val ownerKey: OwnerKey,
    val programId: ProgramId,
    val displayAlias: String?,
    val role: RecoveryTrackRole,
    val status: RecoveryTrackStatus,
    val startedAt: Instant,
    val pausedAt: Instant? = null,
    val maintenanceAt: Instant? = null,
    val archivedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val revision: Long = 0,
) {
    init {
        require(displayAlias == null || displayAlias.isNotBlank()) {
            "Display alias must be null or non-blank"
        }
        require(displayAlias == null || displayAlias.length <= MAX_ALIAS_LENGTH) {
            "Display alias must be at most $MAX_ALIAS_LENGTH characters"
        }
        require(revision >= 0) { "Revision must not be negative" }
        require(!updatedAt.isBefore(createdAt)) {
            "updatedAt must not be earlier than createdAt"
        }
        listOfNotNull(pausedAt, maintenanceAt, archivedAt).forEach { lifecycleTime ->
            require(!lifecycleTime.isAfter(updatedAt)) {
                "Lifecycle timestamps must not be later than updatedAt"
            }
        }
        require(role != RecoveryTrackRole.PRIMARY || status.isPrimaryEligible) {
            "Only active or maintenance tracks can be primary"
        }
        when (status) {
            RecoveryTrackStatus.ACTIVE -> {
                require(pausedAt == null && maintenanceAt == null && archivedAt == null)
            }
            RecoveryTrackStatus.PAUSED -> {
                require(pausedAt != null && archivedAt == null)
            }
            RecoveryTrackStatus.MAINTENANCE -> {
                require(maintenanceAt != null && archivedAt == null)
            }
            RecoveryTrackStatus.ARCHIVED -> require(archivedAt != null)
        }
    }

    val isOpen: Boolean get() = status != RecoveryTrackStatus.ARCHIVED

    fun canTransitionTo(target: RecoveryTrackStatus): Boolean = when (status) {
        RecoveryTrackStatus.ACTIVE -> target in setOf(
            RecoveryTrackStatus.PAUSED,
            RecoveryTrackStatus.MAINTENANCE,
            RecoveryTrackStatus.ARCHIVED,
        )
        RecoveryTrackStatus.PAUSED -> target in setOf(
            RecoveryTrackStatus.ACTIVE,
            RecoveryTrackStatus.ARCHIVED,
        )
        RecoveryTrackStatus.MAINTENANCE -> target in setOf(
            RecoveryTrackStatus.ACTIVE,
            RecoveryTrackStatus.PAUSED,
            RecoveryTrackStatus.ARCHIVED,
        )
        RecoveryTrackStatus.ARCHIVED -> false
    }

    fun transitionTo(
        target: RecoveryTrackStatus,
        at: Instant,
    ): RecoveryTrack {
        require(canTransitionTo(target)) { "Invalid transition: $status -> $target" }
        require(!at.isBefore(updatedAt)) { "Transition time must not be earlier than updatedAt" }

        return copy(
            role = if (target.isPrimaryEligible) role else RecoveryTrackRole.SUPPORTING,
            status = target,
            pausedAt = if (target == RecoveryTrackStatus.PAUSED) at else null,
            maintenanceAt = if (target == RecoveryTrackStatus.MAINTENANCE) at else null,
            archivedAt = if (target == RecoveryTrackStatus.ARCHIVED) at else null,
            updatedAt = at,
            revision = revision + 1,
        )
    }

    companion object {
        const val MAX_ALIAS_LENGTH = 80
    }
}

val RecoveryTrackStatus.isPrimaryEligible: Boolean
    get() = this == RecoveryTrackStatus.ACTIVE || this == RecoveryTrackStatus.MAINTENANCE
