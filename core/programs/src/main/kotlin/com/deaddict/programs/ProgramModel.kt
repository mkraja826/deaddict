package com.deaddict.programs

@JvmInline
value class ProgramId private constructor(val value: String) {
    companion object {
        private val valid = Regex("[a-z][a-z0-9_]{2,49}")
        fun of(value: String): ProgramId {
            require(valid.matches(value)) { "Invalid program id: $value" }
            return ProgramId(value)
        }
    }
}

enum class ProgramCategory { SUBSTANCE, DIGITAL, BEHAVIOURAL }

enum class SafetyTier {
    GENERAL_SELF_MANAGEMENT,
    CLINICALLY_SENSITIVE,
    MEDICALLY_HIGH_RISK,
}

enum class MeasurementKind {
    EVENT_COUNT,
    QUANTITY,
    DURATION_MINUTES,
    COST,
    URGE_INTENSITY,
}

data class MeasurementDefinition(
    val kind: MeasurementKind,
    val unit: String,
    val minimum: Double = 0.0,
    val maximum: Double? = null,
) {
    init {
        require(unit.isNotBlank())
        require(minimum >= 0)
        require(maximum == null || maximum >= minimum)
    }
}

sealed interface OnboardingField {
    val key: String
    val required: Boolean

    data class Choice(
        override val key: String,
        override val required: Boolean,
        val options: List<String>,
    ) : OnboardingField {
        init {
            require(options.size >= 2)
            require(options.none(String::isBlank))
        }
    }

    data class PositiveNumber(
        override val key: String,
        override val required: Boolean,
        val unit: String,
    ) : OnboardingField
}

data class OnboardingSchema(val fields: List<OnboardingField>)

data class TrackingDefinition(
    val measurements: List<MeasurementDefinition>,
    val allowsPrivateNote: Boolean = true,
) {
    init {
        require(measurements.isNotEmpty())
        require(measurements.map { it.kind }.distinct().size == measurements.size)
    }
}

sealed interface ReplacementAction {
    data object Breathe : ReplacementAction
    data object ChangeLocation : ReplacementAction
    data object DrinkWater : ReplacementAction
    data object ShortWalk : ReplacementAction
    data object ContactSupport : ReplacementAction
    data object DelayAndRecheck : ReplacementAction
    data object RemoveAccess : ReplacementAction
}

data class RescueDefinition(
    val pauseSeconds: Int = 60,
    val replacementActions: List<ReplacementAction>,
) {
    init {
        require(pauseSeconds in 60..90)
        require(replacementActions.distinct().size >= 3)
    }
}

enum class InsightSignal { TIME_OF_DAY, DAY_OF_WEEK, TRIGGER, CONTEXT, TREND, COST, DURATION }

data class InsightDefinition(val enabledSignals: Set<InsightSignal>) {
    init {
        require(enabledSignals.isNotEmpty())
    }
}

data class NotificationDefinition(
    val supportsRiskPeriodReminder: Boolean,
    val supportsNearLimitWarning: Boolean,
)

data class SafetyDefinition(
    val tier: SafetyTier,
    val professionalHelpPrompt: Boolean,
    val emergencyEscalation: Boolean,
    val prohibitedGuidance: Set<ProhibitedGuidance>,
) {
    init {
        if (tier == SafetyTier.MEDICALLY_HIGH_RISK) {
            require(professionalHelpPrompt)
            require(emergencyEscalation)
            require(prohibitedGuidance.containsAll(ProhibitedGuidance.entries))
        }
    }
}

enum class ProhibitedGuidance {
    DETOX_PLAN,
    TAPER_SCHEDULE,
    MEDICATION_ADVICE,
    DIAGNOSIS,
    SUDDEN_STOP_SAFETY_CLAIM,
    AUTOMATED_TREATMENT,
}

data class ProgramDefinition(
    val id: ProgramId,
    val displayName: String,
    val category: ProgramCategory,
    val safety: SafetyDefinition,
    val onboarding: OnboardingSchema,
    val tracking: TrackingDefinition,
    val rescue: RescueDefinition,
    val insights: InsightDefinition,
    val notifications: NotificationDefinition,
) {
    init {
        require(displayName.isNotBlank())
        require(displayName.length <= 80)
    }
}

