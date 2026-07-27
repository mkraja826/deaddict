package com.deaddict.programs

object StandardPrograms {
    private val commonInsights = InsightDefinition(
        setOf(
            InsightSignal.TIME_OF_DAY,
            InsightSignal.DAY_OF_WEEK,
            InsightSignal.TRIGGER,
            InsightSignal.TREND,
        ),
    )
    private val commonRescue = RescueDefinition(
        replacementActions = listOf(
            ReplacementAction.Breathe,
            ReplacementAction.ChangeLocation,
            ReplacementAction.DelayAndRecheck,
            ReplacementAction.ContactSupport,
        ),
    )
    private val quantityTracking = TrackingDefinition(
        listOf(
            MeasurementDefinition(MeasurementKind.EVENT_COUNT, "times"),
            MeasurementDefinition(MeasurementKind.QUANTITY, "units"),
            MeasurementDefinition(MeasurementKind.COST, "local_currency"),
            MeasurementDefinition(MeasurementKind.URGE_INTENSITY, "1_to_5", 1.0, 5.0),
        ),
    )
    private val durationTracking = TrackingDefinition(
        listOf(
            MeasurementDefinition(MeasurementKind.EVENT_COUNT, "sessions"),
            MeasurementDefinition(MeasurementKind.DURATION_MINUTES, "minutes"),
            MeasurementDefinition(MeasurementKind.URGE_INTENSITY, "1_to_5", 1.0, 5.0),
        ),
    )
    private val generalOnboarding = OnboardingSchema(
        listOf(
            OnboardingField.Choice("goal", true, listOf("reduce", "stop", "understand")),
            OnboardingField.Choice("frequency", true, listOf("daily", "weekly", "occasionally")),
        ),
    )

    val all: List<ProgramDefinition> = listOf(
        substance("nicotine_tobacco", "Nicotine and tobacco", SafetyTier.CLINICALLY_SENSITIVE),
        substance("alcohol", "Alcohol", SafetyTier.MEDICALLY_HIGH_RISK),
        substance("cannabis", "Cannabis", SafetyTier.CLINICALLY_SENSITIVE),
        substance("opioids", "Opioids", SafetyTier.MEDICALLY_HIGH_RISK),
        substance("stimulants", "Stimulants", SafetyTier.MEDICALLY_HIGH_RISK),
        substance("sedatives", "Sedatives and sleeping medicines", SafetyTier.MEDICALLY_HIGH_RISK),
        substance("inhalants", "Inhalants", SafetyTier.MEDICALLY_HIGH_RISK),
        substance("caffeine", "Caffeine", SafetyTier.GENERAL_SELF_MANAGEMENT),
        digital("smartphone_overuse", "Smartphone overuse"),
        digital("social_media", "Social media"),
        digital("short_videos", "Short videos"),
        digital("internet", "Internet"),
        digital("gaming", "Gaming"),
        behavioural("gambling", "Gambling", SafetyTier.CLINICALLY_SENSITIVE),
        behavioural("pornography", "Pornography", SafetyTier.CLINICALLY_SENSITIVE),
        behavioural("compulsive_sexual_behaviour", "Compulsive sexual behaviour", SafetyTier.CLINICALLY_SENSITIVE),
        behavioural("shopping", "Shopping"),
        behavioural("sugar_junk_food", "Sugar and junk food", SafetyTier.CLINICALLY_SENSITIVE),
        behavioural("emotional_eating", "Emotional eating", SafetyTier.CLINICALLY_SENSITIVE),
        digital("streaming", "Streaming"),
        digital("news_doomscrolling", "News and doomscrolling"),
        behavioural("work_compulsion", "Work compulsion"),
        behavioural("exercise_compulsion", "Exercise compulsion"),
        behavioural("relationship_dependency", "Relationship dependency", SafetyTier.CLINICALLY_SENSITIVE),
        behavioural("risk_taking", "Risk-taking", SafetyTier.CLINICALLY_SENSITIVE),
        behavioural("custom_habit", "Custom habit"),
    )

    private fun substance(id: String, name: String, tier: SafetyTier) =
        definition(id, name, ProgramCategory.SUBSTANCE, tier, quantityTracking)

    private fun digital(id: String, name: String) =
        definition(id, name, ProgramCategory.DIGITAL, SafetyTier.GENERAL_SELF_MANAGEMENT, durationTracking)

    private fun behavioural(
        id: String,
        name: String,
        tier: SafetyTier = SafetyTier.GENERAL_SELF_MANAGEMENT,
    ) = definition(id, name, ProgramCategory.BEHAVIOURAL, tier, quantityTracking)

    private fun definition(
        id: String,
        name: String,
        category: ProgramCategory,
        tier: SafetyTier,
        tracking: TrackingDefinition,
    ) = ProgramDefinition(
        id = ProgramId.of(id),
        displayName = name,
        category = category,
        safety = safety(tier),
        onboarding = generalOnboarding,
        tracking = tracking,
        rescue = commonRescue,
        insights = commonInsights,
        notifications = NotificationDefinition(
            supportsRiskPeriodReminder = true,
            supportsNearLimitWarning = category == ProgramCategory.DIGITAL,
        ),
    )

    private fun safety(tier: SafetyTier) = SafetyDefinition(
        tier = tier,
        professionalHelpPrompt = tier != SafetyTier.GENERAL_SELF_MANAGEMENT,
        emergencyEscalation = tier == SafetyTier.MEDICALLY_HIGH_RISK,
        prohibitedGuidance = if (tier == SafetyTier.MEDICALLY_HIGH_RISK) {
            ProhibitedGuidance.entries.toSet()
        } else {
            emptySet()
        },
    )
}

