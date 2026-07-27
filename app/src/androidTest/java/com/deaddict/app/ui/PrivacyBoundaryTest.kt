package com.deaddict.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.deaddict.app.ui.theme.DeAddictTheme
import org.junit.Rule
import org.junit.Test

class PrivacyBoundaryTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lockedAppDoesNotExposeRecoveryUi() {
        composeRule.setContent {
            DeAddictTheme {
                DeAddictRoot(
                    state = AppUiState(isLoading = false),
                    isAppUnlocked = false,
                    onTabSelected = {},
                    onProgramSelected = {},
                    onTrackingRecorded = { _, _ -> },
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
                )
            }
        }

        composeRule.onNodeWithText("DeAddict is locked").assertIsDisplayed()
        composeRule.onNodeWithText("Track").assertDoesNotExist()
        composeRule.onNodeWithText("Rescue").assertDoesNotExist()
    }
}
