package com.deaddict.app.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.deaddict.app.coach.RookTone
import com.deaddict.model.RecoveryGoalType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore("onboarding_draft")

enum class OnboardingStep {
    WELCOME,
    PRIVACY,
    USAGE_MODE,
    MOTIVATION,
    PRIMARY_PROGRAM,
    SAFETY_DISCLOSURE,
    GOAL_TYPE,
    GOAL_DETAILS,
    BASELINE,
    TRIGGERS,
    SUPPORTING_TRACKS,
    ROOK,
    NOTIFICATIONS,
    FIRST_CHECK_IN,
    SUMMARY,
    COMPLETE,
}

enum class OnboardingUsageMode {
    PRIVATE_ON_DEVICE,
    SIGN_IN_AND_SYNC,
}

data class OnboardingDraft(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val privacyAccepted: Boolean = false,
    val usageMode: OnboardingUsageMode? = null,
    val motivation: String = "",
    val primaryProgramId: String? = null,
    val safetyAcknowledged: Boolean = false,
    val goalType: RecoveryGoalType? = null,
    val goalTarget: Double? = null,
    val goalUnit: String? = null,
    val baselineValue: Double? = null,
    val triggerKeys: Set<String> = emptySet(),
    val supportingProgramIds: Set<String> = emptySet(),
    val rookTone: RookTone = RookTone.DIRECT,
    val notificationsEnabled: Boolean = false,
) {
    val isComplete: Boolean get() = step == OnboardingStep.COMPLETE
}

class OnboardingCoordinator {
    fun canContinue(draft: OnboardingDraft): Boolean = when (draft.step) {
        OnboardingStep.WELCOME -> true
        OnboardingStep.PRIVACY -> draft.privacyAccepted
        OnboardingStep.USAGE_MODE -> draft.usageMode != null
        OnboardingStep.MOTIVATION -> draft.motivation.trim().isNotEmpty()
        OnboardingStep.PRIMARY_PROGRAM -> !draft.primaryProgramId.isNullOrBlank()
        OnboardingStep.SAFETY_DISCLOSURE -> draft.safetyAcknowledged
        OnboardingStep.GOAL_TYPE -> draft.goalType != null
        OnboardingStep.GOAL_DETAILS -> when (draft.goalType) {
            RecoveryGoalType.REDUCE_QUANTITY,
            RecoveryGoalType.DAILY_LIMIT,
            RecoveryGoalType.WEEKLY_LIMIT,
            RecoveryGoalType.TIME_LIMIT,
            RecoveryGoalType.SPENDING_LIMIT,
            RecoveryGoalType.DELAY_FIRST_USE,
            -> draft.goalTarget?.let { it.isFinite() && it > 0 } == true && !draft.goalUnit.isNullOrBlank()

            else -> true
        }
        OnboardingStep.BASELINE,
        OnboardingStep.TRIGGERS,
        OnboardingStep.SUPPORTING_TRACKS,
        OnboardingStep.ROOK,
        OnboardingStep.NOTIFICATIONS,
        OnboardingStep.FIRST_CHECK_IN,
        -> true

        OnboardingStep.SUMMARY ->
            draft.privacyAccepted && !draft.primaryProgramId.isNullOrBlank() && draft.goalType != null

        OnboardingStep.COMPLETE -> false
    }

    fun next(draft: OnboardingDraft): OnboardingDraft {
        require(canContinue(draft)) { "The current onboarding step is incomplete" }
        val index = ORDER.indexOf(draft.step)
        require(index >= 0 && index < ORDER.lastIndex) { "Onboarding is already complete" }
        return draft.copy(step = ORDER[index + 1])
    }

    fun previous(draft: OnboardingDraft): OnboardingDraft {
        val index = ORDER.indexOf(draft.step)
        if (index <= 0) return draft
        return draft.copy(step = ORDER[index - 1])
    }

    companion object {
        val ORDER: List<OnboardingStep> = OnboardingStep.entries
    }
}

@Singleton
class OnboardingDraftStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val draft: Flow<OnboardingDraft> = context.onboardingDataStore.data.map { values ->
        OnboardingDraft(
            step = values[STEP]
                ?.let { stored -> runCatching { OnboardingStep.valueOf(stored) }.getOrNull() }
                ?: OnboardingStep.WELCOME,
            privacyAccepted = values[PRIVACY_ACCEPTED] ?: false,
            usageMode = values[USAGE_MODE]
                ?.let { stored -> runCatching { OnboardingUsageMode.valueOf(stored) }.getOrNull() },
            motivation = values[MOTIVATION].orEmpty(),
            primaryProgramId = values[PRIMARY_PROGRAM],
            safetyAcknowledged = values[SAFETY_ACKNOWLEDGED] ?: false,
            goalType = values[GOAL_TYPE]
                ?.let { stored -> runCatching { RecoveryGoalType.valueOf(stored) }.getOrNull() },
            goalTarget = values[GOAL_TARGET],
            goalUnit = values[GOAL_UNIT],
            baselineValue = values[BASELINE],
            triggerKeys = values[TRIGGERS].orEmpty(),
            supportingProgramIds = values[SUPPORTING_PROGRAMS].orEmpty(),
            rookTone = values[ROOK_TONE]
                ?.let { stored -> runCatching { RookTone.valueOf(stored) }.getOrNull() }
                ?: RookTone.DIRECT,
            notificationsEnabled = values[NOTIFICATIONS_ENABLED] ?: false,
        )
    }

    suspend fun current(): OnboardingDraft = draft.first()

    suspend fun save(value: OnboardingDraft) {
        context.onboardingDataStore.edit { values ->
            values[STEP] = value.step.name
            values[PRIVACY_ACCEPTED] = value.privacyAccepted
            value.usageMode?.let { values[USAGE_MODE] = it.name } ?: values.remove(USAGE_MODE)
            values[MOTIVATION] = value.motivation
            value.primaryProgramId?.let { values[PRIMARY_PROGRAM] = it } ?: values.remove(PRIMARY_PROGRAM)
            values[SAFETY_ACKNOWLEDGED] = value.safetyAcknowledged
            value.goalType?.let { values[GOAL_TYPE] = it.name } ?: values.remove(GOAL_TYPE)
            value.goalTarget?.let { values[GOAL_TARGET] = it } ?: values.remove(GOAL_TARGET)
            value.goalUnit?.let { values[GOAL_UNIT] = it } ?: values.remove(GOAL_UNIT)
            value.baselineValue?.let { values[BASELINE] = it } ?: values.remove(BASELINE)
            values[TRIGGERS] = value.triggerKeys
            values[SUPPORTING_PROGRAMS] = value.supportingProgramIds
            values[ROOK_TONE] = value.rookTone.name
            values[NOTIFICATIONS_ENABLED] = value.notificationsEnabled
        }
    }

    suspend fun update(transform: (OnboardingDraft) -> OnboardingDraft) {
        save(transform(current()))
    }

    suspend fun reset() {
        context.onboardingDataStore.edit { it.clear() }
    }

    private companion object {
        val STEP = stringPreferencesKey("step")
        val PRIVACY_ACCEPTED = booleanPreferencesKey("privacy_accepted")
        val USAGE_MODE = stringPreferencesKey("usage_mode")
        val MOTIVATION = stringPreferencesKey("motivation")
        val PRIMARY_PROGRAM = stringPreferencesKey("primary_program")
        val SAFETY_ACKNOWLEDGED = booleanPreferencesKey("safety_acknowledged")
        val GOAL_TYPE = stringPreferencesKey("goal_type")
        val GOAL_TARGET = doublePreferencesKey("goal_target")
        val GOAL_UNIT = stringPreferencesKey("goal_unit")
        val BASELINE = doublePreferencesKey("baseline")
        val TRIGGERS = stringSetPreferencesKey("triggers")
        val SUPPORTING_PROGRAMS = stringSetPreferencesKey("supporting_programs")
        val ROOK_TONE = stringPreferencesKey("rook_tone")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }
}
