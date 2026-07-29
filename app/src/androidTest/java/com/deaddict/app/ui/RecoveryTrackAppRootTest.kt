package com.deaddict.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.deaddict.app.coach.RookTone
import com.deaddict.app.onboarding.OnboardingUsageMode
import com.deaddict.app.rescue.RescueFlowState
import com.deaddict.app.rescue.RescueStep
import com.deaddict.app.ui.theme.DeAddictTheme
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.DefaultProgramRegistry
import com.deaddict.programs.ProgramDefinition
import com.deaddict.programs.ProgramId
import org.junit.Rule
import org.junit.Test

class RecoveryTrackAppRootTest {
    @get:Rule
    val compose = createComposeRule()

    private val registry = DefaultProgramRegistry()
    private val gaming = registry.find(ProgramId.of("gaming"))!!
    private val caffeine = registry.find(ProgramId.of("caffeine"))!!
    private val gamingTrack = RecoveryTrackUi(
        id = GAMING_TRACK_ID,
        program = gaming,
        displayAlias = null,
        role = RecoveryTrackRole.PRIMARY,
        status = RecoveryTrackStatus.ACTIVE,
    )
    private val caffeineTrack = RecoveryTrackUi(
        id = CAFFEINE_TRACK_ID,
        program = caffeine,
        displayAlias = null,
        role = RecoveryTrackRole.SUPPORTING,
        status = RecoveryTrackStatus.ACTIVE,
    )

    @Test
    fun productionNavigationUsesFiveFinalLabels() {
        setRoot(baseState(selectedTab = AppTab.TODAY))

        listOf("Today", "Tracks", "Tools", "Insights", "You").forEach { label ->
            compose.onNodeWithText(label).assertExists()
        }
    }

    @Test
    fun switchingTracksChangesTheExplicitTrackingDestination() {
        var state by mutableStateOf(baseState(selectedTab = AppTab.TRACKS))
        compose.setContent {
            DeAddictTheme {
                RootHarness(
                    state = state,
                    onTrackSelected = { selected ->
                        state = state.copy(selectedRecoveryTrackId = selected)
                    },
                )
            }
        }

        compose.onNodeWithText("Saving to Gaming").assertExists()
        compose.onNodeWithText("Caffeine").performClick()
        compose.onNodeWithText("Saving to Caffeine").assertExists()
    }

    @Test
    fun activeRescueDisplaysItsOwningTrackNotTheCurrentlySelectedTrack() {
        val state = baseState(
            selectedTab = AppTab.TOOLS,
            selectedRecoveryTrackId = CAFFEINE_TRACK_ID,
        ).copy(
            rescue = RescueFlowState(
                step = RescueStep.PAUSE,
                recoveryTrackId = GAMING_TRACK_ID,
                program = gaming,
                startedAtEpochMillis = 1_000L,
                secondsRemaining = 0,
            ),
        )

        setRoot(state)

        compose.onNodeWithText("Rescue for Gaming").assertExists()
    }

    private fun setRoot(state: AppUiState) {
        compose.setContent {
            DeAddictTheme { RootHarness(state = state) }
        }
    }

    private fun baseState(
        selectedTab: AppTab,
        selectedRecoveryTrackId: String = GAMING_TRACK_ID,
    ): AppUiState = AppUiState(
        isLoading = false,
        ownerKey = "guest:test-profile",
        selectedTab = selectedTab,
        availablePrograms = listOf(gaming, caffeine),
        recoveryTracks = listOf(gamingTrack, caffeineTrack),
        selectedRecoveryTrackId = selectedRecoveryTrackId,
    )

    @androidx.compose.runtime.Composable
    private fun RootHarness(
        state: AppUiState,
        onTrackSelected: (String) -> Unit = {},
    ) {
        RecoveryTrackAppRoot(
            state = state,
            isAppUnlocked = true,
            onTabSelected = {},
            onProgramSelected = {},
            onTrackSelected = onTrackSelected,
            onMakePrimary = {},
            onPauseTrack = {},
            onResumeTrack = {},
            onMaintenanceTrack = {},
            onArchiveTrack = {},
            onTrackingRecorded = {},
            onRequestUsageAccess = {},
            onBeginRescue = {},
            onRescueTick = {},
            onRescueContinue = {},
            onRescueInitialUrge = {},
            onRescueTrigger = {},
            onRescueAction = {},
            onRescueFinalUrge = {},
            onRescueComplete = {},
            onRescueReset = {},
            onEnableDailyNotifications = {},
            onDisableDailyNotifications = {},
            onEnableBiometricLock = {},
            onDisableBiometricLock = {},
            onScreenProtectionChanged = {},
            onAnalyticsChanged = {},
            onUsageMonitoringChanged = {},
            onDeleteLocalData = {},
            onPurchasePlus = {},
            onRestorePurchases = {},
            onOnboardingNext = {},
            onOnboardingBack = {},
            onOnboardingPrivacyChanged = {},
            onOnboardingUsageModeChanged = { _: OnboardingUsageMode -> },
            onOnboardingMotivationChanged = {},
            onOnboardingPrimaryProgramChanged = { _: ProgramDefinition -> },
            onOnboardingSafetyChanged = {},
            onOnboardingGoalChanged = { _: RecoveryGoalType -> },
            onOnboardingGoalDetailsChanged = { _, _ -> },
            onOnboardingBaselineChanged = {},
            onOnboardingTriggerToggled = {},
            onOnboardingRookToneChanged = { _: RookTone -> },
            onOnboardingNotificationsChanged = {},
            onOnboardingComplete = {},
        )
    }

    private companion object {
        const val GAMING_TRACK_ID = "00000000-0000-0000-0000-000000000101"
        const val CAFFEINE_TRACK_ID = "00000000-0000-0000-0000-000000000102"
    }
}
