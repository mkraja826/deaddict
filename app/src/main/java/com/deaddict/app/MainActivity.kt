package com.deaddict.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deaddict.app.ui.AccountDeletionAction
import com.deaddict.app.ui.AppTab
import com.deaddict.app.ui.AppViewModel
import com.deaddict.app.ui.DailyCheckInTodayScreen
import com.deaddict.app.ui.DailyCheckInViewModel
import com.deaddict.app.ui.RecoveryTrackAppRoot
import com.deaddict.app.ui.theme.DeAddictTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private val dailyCheckInViewModel: DailyCheckInViewModel by viewModels()
    private var biometricAuthenticated by mutableStateOf(false)
    private var biometricPromptShowing = false
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.setDailyNotificationsEnabled(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val dailyCheckInState by dailyCheckInViewModel.state.collectAsStateWithLifecycle()
            val lockRequired = state.privacyPreferences.biometricLockEnabled
            val isAppUnlocked = !lockRequired || biometricAuthenticated
            val showDailyCheckIn = isAppUnlocked &&
                !state.isLoading &&
                !state.requiresOnboarding &&
                state.selectedTab == AppTab.TODAY
            SideEffect {
                if (state.privacyPreferences.screenProtectionEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            LaunchedEffect(lockRequired, biometricAuthenticated) {
                if (lockRequired && !biometricAuthenticated && !biometricPromptShowing) {
                    authenticate { biometricAuthenticated = true }
                }
            }
            DeAddictTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showDailyCheckIn) {
                        DailyCheckInTodayScreen(
                            appState = state,
                            checkInState = dailyCheckInState,
                            onTabSelected = viewModel::selectTab,
                            onSave = dailyCheckInViewModel::save,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        RecoveryTrackAppRoot(
                            state = state,
                            isAppUnlocked = isAppUnlocked,
                            onTabSelected = viewModel::selectTab,
                            onProgramSelected = viewModel::activateProgram,
                            onTrackSelected = viewModel::selectRecoveryTrack,
                            onMakePrimary = viewModel::makePrimary,
                            onPauseTrack = viewModel::pauseTrack,
                            onResumeTrack = viewModel::resumeTrack,
                            onMaintenanceTrack = viewModel::moveTrackToMaintenance,
                            onArchiveTrack = viewModel::archiveTrack,
                            onTrackingRecorded = viewModel::recordTrackingSelected,
                            onRequestUsageAccess = {
                                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            },
                            onBeginRescue = viewModel::beginRescue,
                            onRescueTick = viewModel::tickRescuePause,
                            onRescueContinue = viewModel::continueRescue,
                            onRescueInitialUrge = viewModel::setRescueInitialUrge,
                            onRescueTrigger = viewModel::chooseRescueTrigger,
                            onRescueAction = viewModel::chooseRescueAction,
                            onRescueFinalUrge = viewModel::setRescueFinalUrge,
                            onRescueComplete = viewModel::completeRescue,
                            onRescueReset = viewModel::resetRescue,
                            onEnableDailyNotifications = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setDailyNotificationsEnabled(true)
                                }
                            },
                            onDisableDailyNotifications = {
                                viewModel.setDailyNotificationsEnabled(false)
                            },
                            onEnableBiometricLock = {
                                authenticate {
                                    biometricAuthenticated = true
                                    viewModel.setBiometricLock(true)
                                }
                            },
                            onDisableBiometricLock = {
                                viewModel.setBiometricLock(false)
                                biometricAuthenticated = false
                            },
                            onScreenProtectionChanged = viewModel::setScreenProtection,
                            onAnalyticsChanged = viewModel::setAnalyticsEnabled,
                            onUsageMonitoringChanged = viewModel::setUsageMonitoringEnabled,
                            onDeleteLocalData = viewModel::deleteLocalRecoveryData,
                            onPurchasePlus = { offerToken ->
                                viewModel.purchasePlus(this@MainActivity, offerToken)
                            },
                            onRestorePurchases = viewModel::refreshBilling,
                            onOnboardingNext = viewModel::onboardingNext,
                            onOnboardingBack = viewModel::onboardingBack,
                            onOnboardingPrivacyChanged = viewModel::setOnboardingPrivacyAccepted,
                            onOnboardingUsageModeChanged = viewModel::setOnboardingUsageMode,
                            onOnboardingMotivationChanged = viewModel::setOnboardingMotivation,
                            onOnboardingPrimaryProgramChanged = viewModel::setOnboardingPrimaryProgram,
                            onOnboardingSafetyChanged = viewModel::acknowledgeOnboardingSafety,
                            onOnboardingGoalChanged = viewModel::setOnboardingGoal,
                            onOnboardingGoalDetailsChanged = viewModel::setOnboardingGoalDetails,
                            onOnboardingBaselineChanged = viewModel::setOnboardingBaseline,
                            onOnboardingTriggerToggled = viewModel::toggleOnboardingTrigger,
                            onOnboardingRookToneChanged = viewModel::setOnboardingRookTone,
                            onOnboardingNotificationsChanged = viewModel::setOnboardingNotifications,
                            onOnboardingComplete = viewModel::completeOnboarding,
                        )
                    }
                    AccountDeletionAction(
                        visible = isAppUnlocked &&
                            !state.isLoading &&
                            !state.requiresOnboarding &&
                            state.selectedTab == AppTab.YOU &&
                            state.accountDeletionAvailable,
                        inProgress = state.accountDeletionInProgress,
                        onConfirm = viewModel::deleteAccount,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDigitalUsage()
        dailyCheckInViewModel.refreshDate()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) biometricAuthenticated = false
    }

    private fun authenticate(onSuccess: () -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (
            BiometricManager.from(this).canAuthenticate(authenticators) !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) return
        biometricPromptShowing = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    biometricPromptShowing = false
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    biometricPromptShowing = false
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_unlock_title))
                .setSubtitle(getString(R.string.biometric_unlock_subtitle))
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }
}
