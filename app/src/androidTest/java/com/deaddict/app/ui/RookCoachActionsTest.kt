package com.deaddict.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextContaining
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.deaddict.app.coach.RookPreferences
import com.deaddict.app.coach.RookTone
import com.deaddict.app.ui.theme.DeAddictTheme
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.DefaultProgramRegistry
import com.deaddict.programs.ProgramId
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RookCoachActionsTest {
    @get:Rule
    val compose = createComposeRule()

    private val gaming = DefaultProgramRegistry().find(ProgramId.of("gaming"))!!
    private val track = RecoveryTrackUi(
        id = TRACK_ID,
        program = gaming,
        displayAlias = null,
        role = RecoveryTrackRole.PRIMARY,
        status = RecoveryTrackStatus.ACTIVE,
    )

    @Test
    fun savedOutcomeOpensTrackAwareRookMessage() {
        val appState = AppUiState(
            isLoading = false,
            selectedTab = AppTab.TODAY,
            recoveryTracks = listOf(track),
            selectedRecoveryTrackId = TRACK_ID,
            rookPreferences = RookPreferences(tone = RookTone.DIRECT),
        )
        val dailyState = DailyCheckInUiState(
            isLoading = false,
            localDateEpochDay = 20_000L,
            updatedAtEpochMillis = 1_000L,
            entries = mapOf(
                TRACK_ID to DailyTrackCheckInUi(
                    recoveryTrackId = TRACK_ID,
                    outcome = TrackCheckInOutcome.GOAL_MET,
                    measuredValue = null,
                    unitKey = null,
                    peakUrge = 2,
                    privateNote = "This must never be analyzed",
                ),
            ),
        )

        compose.setContent {
            DeAddictTheme {
                RookCoachAction(
                    appState = appState,
                    dailyCheckInState = dailyState,
                    insightsState = InsightsControlsUiState(isLoading = false),
                    visible = true,
                )
            }
        }

        compose.onNodeWithTag("rook_coach_action").performClick()
        compose.onNode(hasText("Rook · Goal met")).assertExists()
        compose.onNode(hasTextContaining("Gaming")).assertExists()
        compose.onNode(hasTextContaining("This must never be analyzed")).assertDoesNotExist()
    }

    @Test
    fun selectedTrackCanReturnToDefaultTone() {
        var selectedOverride by mutableStateOf<RookTone?>(RookTone.QUIET)
        compose.setContent {
            DeAddictTheme {
                RookSettingsAction(
                    visible = true,
                    preferences = RookPreferences(
                        tone = RookTone.DIRECT,
                        trackTones = mapOf(TRACK_ID to selectedOverride).filterValues { it != null }
                            .mapValues { checkNotNull(it.value) },
                    ),
                    selectedTrack = track,
                    onEnabledChanged = {},
                    onDefaultToneChanged = {},
                    onAvatarVisibleChanged = {},
                    onTrackToneChanged = { _, tone -> selectedOverride = tone },
                )
            }
        }

        compose.onNodeWithTag("rook_settings_action").performClick()
        compose.onNodeWithTag("rook_tone_default").performClick()

        assertNull(selectedOverride)
    }

    private companion object {
        const val TRACK_ID = "00000000-0000-0000-0000-000000000909"
    }
}
