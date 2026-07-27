package com.deaddict.app

import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deaddict.app.ui.AppViewModel
import com.deaddict.app.ui.DeAddictRoot
import com.deaddict.app.ui.theme.DeAddictTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.view.WindowManager

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: AppViewModel by viewModels()
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
            val lockRequired = state.privacyPreferences.biometricLockEnabled
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
                DeAddictRoot(
                    state = state,
                    isAppUnlocked = !lockRequired || biometricAuthenticated,
                    onTabSelected = viewModel::selectTab,
                    onProgramSelected = viewModel::activateProgram,
                    onTrackingRecorded = viewModel::recordTracking,
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
                        viewModel.purchasePlus(this, offerToken)
                    },
                    onRestorePurchases = viewModel::refreshBilling,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDigitalUsage()
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
                .setTitle("Unlock DeAddict")
                .setSubtitle("Confirm it’s you")
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }
}
