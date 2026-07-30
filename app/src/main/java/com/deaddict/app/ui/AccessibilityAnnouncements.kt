package com.deaddict.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics

@Composable
internal fun AccessibilityAnnouncements(
    selectedTab: AppTab,
    dailyCheckInState: DailyCheckInUiState,
    insightsState: InsightsControlsUiState,
) {
    val view = LocalView.current
    val announcement = when (selectedTab) {
        AppTab.TODAY -> when {
            dailyCheckInState.feedback != null -> dailyCheckInState.feedback
            dailyCheckInState.isSaving -> "Saving today’s check-in."
            dailyCheckInState.isLoading -> "Loading today’s check-in."
            else -> null
        }

        AppTab.INSIGHTS -> when {
            insightsState.errorMessage != null -> insightsState.errorMessage
            insightsState.isLoading -> "Refreshing Insights for ${insightsState.window.days} days."
            insightsState.insights != null -> "Insights ready for ${insightsState.window.days} days."
            else -> null
        }

        else -> null
    }

    LaunchedEffect(announcement) {
        announcement?.takeIf(String::isNotBlank)?.let(view::announceForAccessibility)
    }
}

internal fun Modifier.recoveryPane(title: String): Modifier = semantics {
    paneTitle = title
}
