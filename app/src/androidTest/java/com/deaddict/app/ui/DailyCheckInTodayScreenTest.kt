package com.deaddict.app.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.deaddict.app.ui.theme.DeAddictTheme
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.DefaultProgramRegistry
import com.deaddict.programs.ProgramId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class DailyCheckInTodayScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val registry = DefaultProgramRegistry()
    private val gaming = checkNotNull(registry.find(ProgramId.of("gaming")))
    private val caffeine = checkNotNull(registry.find(ProgramId.of("caffeine")))

    @Test
    fun saveRequiresAnIndependentAccessibleOutcomeForEveryEligibleTrack() {
        var saved: DailyCheckInSubmission? = null
        val state = AppUiState(
            isLoading = false,
            ownerKey = "guest:test-profile",
            selectedTab = AppTab.TODAY,
            availablePrograms = listOf(gaming, caffeine),
            recoveryTracks = listOf(
                RecoveryTrackUi(
                    id = GAMING_TRACK_ID,
                    program = gaming,
                    displayAlias = null,
                    role = RecoveryTrackRole.PRIMARY,
                    status = RecoveryTrackStatus.ACTIVE,
                ),
                RecoveryTrackUi(
                    id = CAFFEINE_TRACK_ID,
                    program = caffeine,
                    displayAlias = null,
                    role = RecoveryTrackRole.SUPPORTING,
                    status = RecoveryTrackStatus.MAINTENANCE,
                ),
            ),
            selectedRecoveryTrackId = GAMING_TRACK_ID,
        )

        compose.setContent {
            DeAddictTheme {
                DailyCheckInTodayScreen(
                    appState = state,
                    checkInState = DailyCheckInUiState(
                        isLoading = false,
                        localDateEpochDay = 20_000L,
                    ),
                    onTabSelected = {},
                    onSave = { saved = it },
                )
            }
        }

        compose.onNodeWithContentDescription("${gaming.displayName}, Goal met")
            .assertExists()
        compose.onNodeWithContentDescription("${caffeine.displayName}, Partly met")
            .assertExists()
        compose.onNodeWithContentDescription("Mood 1 of 5")
            .assertExists()

        compose.onNodeWithTag("save_daily_check_in")
            .performScrollTo()
            .assertIsNotEnabled()

        compose.onNodeWithTag(
            "outcome_${GAMING_TRACK_ID}_${TrackCheckInOutcome.GOAL_MET.name}",
        ).performScrollTo().performClick()
        compose.onNodeWithTag("save_daily_check_in")
            .performScrollTo()
            .assertIsNotEnabled()

        compose.onNodeWithTag(
            "outcome_${CAFFEINE_TRACK_ID}_${TrackCheckInOutcome.GOAL_PARTLY_MET.name}",
        ).performScrollTo().performClick()
        compose.onNodeWithTag("save_daily_check_in")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        compose.runOnIdle {
            val submission = assertNotNull(saved).let { checkNotNull(saved) }
            assertEquals(2, submission.entries.size)
            assertEquals(
                setOf(GAMING_TRACK_ID, CAFFEINE_TRACK_ID),
                submission.entries.map { it.recoveryTrackId }.toSet(),
            )
        }
    }

    private companion object {
        const val GAMING_TRACK_ID = "00000000-0000-0000-0000-000000000301"
        const val CAFFEINE_TRACK_ID = "00000000-0000-0000-0000-000000000302"
    }
}
