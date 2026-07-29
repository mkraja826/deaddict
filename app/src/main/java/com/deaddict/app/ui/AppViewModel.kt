package com.deaddict.app.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deaddict.app.billing.BillingUiState
import com.deaddict.app.billing.PlayBillingManager
import com.deaddict.app.coach.RookPreferenceStore
import com.deaddict.app.coach.RookPreferences
import com.deaddict.app.coach.RookTone
import com.deaddict.app.insights.LocalInsightsRepository
import com.deaddict.app.insights.SevenDayInsights
import com.deaddict.app.notifications.NotificationPreferenceStore
import com.deaddict.app.notifications.NotificationPreferences
import com.deaddict.app.notifications.NotificationScheduler
import com.deaddict.app.privacy.AccountDeletionCoordinator
import com.deaddict.app.privacy.PrivacyPreferenceStore
import com.deaddict.app.privacy.PrivacyPreferences
import com.deaddict.app.rescue.RescueFlow
import com.deaddict.app.rescue.RescueFlowState
import com.deaddict.app.usage.DailyUsageEstimate
import com.deaddict.app.usage.DigitalUsageRepository
import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.RescueOutcome
import com.deaddict.database.entity.TrackingEventKind
import com.deaddict.database.repository.LocalProgramRepository
import com.deaddict.database.repository.LocalRescueRepository
import com.deaddict.database.repository.LocalTrackingRepository
import com.deaddict.database.repository.NewRescueSession
import com.deaddict.database.repository.NewTrackingEvent
import com.deaddict.database.repository.SyncPolicy
import com.deaddict.programs.ProgramDefinition
import com.deaddict.programs.ProgramId
import com.deaddict.programs.ProgramRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab {
    HOME,
    TRACK,
    RESCUE,
    INSIGHTS,
    PROFILE,
}

data class AppUiState(
    val isLoading: Boolean = true,
    val selectedTab: AppTab = AppTab.HOME,
    val availablePrograms: List<ProgramDefinition> = emptyList(),
    val activePrograms: List<ProgramDefinition> = emptyList(),
    val selectedProgramId: String? = null,
    val rookPreferences: RookPreferences = RookPreferences(),
    val message: String? = null,
    val usageAccessGranted: Boolean = false,
    val dailyUsage: DailyUsageEstimate? = null,
    val rescue: RescueFlowState = RescueFlowState(),
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    val insights: SevenDayInsights? = null,
    val privacyPreferences: PrivacyPreferences = PrivacyPreferences(),
    val billing: BillingUiState = BillingUiState(),
    val accountDeletionAvailable: Boolean = false,
    val accountDeletionInProgress: Boolean = false,
) {
    val requiresOnboarding: Boolean
        get() = !isLoading && activePrograms.isEmpty()
}

data class TrackingEntry(
    val kind: TrackingEventKind,
    val value: Double? = null,
    val urgeIntensity: Int? = null,
    val triggerKey: String? = null,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    registry: ProgramRegistry,
    private val programRepository: LocalProgramRepository,
    private val trackingRepository: LocalTrackingRepository,
    private val digitalUsageRepository: DigitalUsageRepository,
    private val rescueRepository: LocalRescueRepository,
    private val notificationPreferenceStore: NotificationPreferenceStore,
    private val notificationScheduler: NotificationScheduler,
    private val insightsRepository: LocalInsightsRepository,
    private val privacyPreferenceStore: PrivacyPreferenceStore,
    private val rookPreferenceStore: RookPreferenceStore,
    private val database: DeAddictDatabase,
    private val billingManager: PlayBillingManager,
    private val accountDeletionCoordinator: AccountDeletionCoordinator,
) : ViewModel() {
    private val selectedTab = MutableStateFlow(AppTab.HOME)
    private val selectedProgramId = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val usageAccessGranted = MutableStateFlow(false)
    private val dailyUsage = MutableStateFlow<DailyUsageEstimate?>(null)
    private val rescueFlow = RescueFlow()
    private val rescueState = MutableStateFlow(RescueFlowState())
    private val insights = MutableStateFlow<SevenDayInsights?>(null)
    private val accountDeletionInProgress = MutableStateFlow(false)
    private val privacyPreferences = privacyPreferenceStore.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PrivacyPreferences(),
        )
    private val rookPreferences = rookPreferenceStore.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RookPreferences(),
        )
    private var rescueStartedAtMillis: Long = 0L
    private val notificationPreferences = notificationPreferenceStore.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationPreferences(),
        )
    private val definitions = registry.all()
    private val definitionsById = definitions.associateBy { it.id.value }

    private val primaryState = combine(
        programRepository.observeActive(),
        selectedTab,
        message,
        selectedProgramId,
    ) { active: List<ActiveProgramEntity>, tab: AppTab, currentMessage: String?, selectedId: String? ->
        val mapped = active.mapNotNull { definitionsById[it.programId] }
        val selected = mapped.firstOrNull { it.id.value == selectedId } ?: mapped.firstOrNull()
        val ordered = if (selected == null) {
            mapped
        } else {
            listOf(selected) + mapped.filterNot { it.id == selected.id }
        }

        AppUiState(
            isLoading = false,
            selectedTab = tab,
            availablePrograms = definitions,
            activePrograms = ordered,
            selectedProgramId = selected?.id?.value,
            message = currentMessage,
        )
    }

    private val stateWithoutInsights = combine(
        primaryState,
        usageAccessGranted,
        dailyUsage,
        rescueState,
        notificationPreferences,
    ) { base, accessGranted, usage, rescue, notifications ->
        base.copy(
            usageAccessGranted = accessGranted,
            dailyUsage = usage,
            rescue = rescue,
            notificationPreferences = notifications,
        )
    }

    private val coachAndDeletion = combine(
        rookPreferences,
        accountDeletionInProgress,
    ) { coach, deleting -> coach to deleting }

    val state: StateFlow<AppUiState> = combine(
        stateWithoutInsights,
        insights,
        privacyPreferences,
        billingManager.state,
        coachAndDeletion,
    ) { base, currentInsights, privacy, billing, coachDeletion ->
        base.copy(
            insights = currentInsights,
            privacyPreferences = privacy,
            rookPreferences = coachDeletion.first,
            billing = billing,
            accountDeletionAvailable = accountDeletionCoordinator.available,
            accountDeletionInProgress = coachDeletion.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(
            availablePrograms = definitions,
            accountDeletionAvailable = accountDeletionCoordinator.available,
        ),
    )

    init {
        refreshDigitalUsage()
        billingManager.connect()
    }

    fun selectTab(tab: AppTab) {
        selectedTab.value = tab
        message.value = null
        if (tab == AppTab.INSIGHTS) refreshInsights()
    }

    fun selectRecoveryTrack(programId: String) {
        if (state.value.activePrograms.none { it.id.value == programId }) return
        selectedProgramId.value = programId
        rescueState.value = rescueFlow.reset()
        insights.value = null
        message.value = null
        if (selectedTab.value == AppTab.INSIGHTS) refreshInsights(programId)
    }

    fun setRookTone(tone: RookTone) {
        viewModelScope.launch { rookPreferenceStore.setTone(tone) }
    }

    fun refreshDigitalUsage() {
        viewModelScope.launch {
            val granted = digitalUsageRepository.hasUsageAccess()
            usageAccessGranted.value = granted
            dailyUsage.value = if (
                granted && privacyPreferences.value.usageMonitoringEnabled
            ) {
                withContext(Dispatchers.Default) { digitalUsageRepository.estimateDay() }
            } else {
                null
            }
        }
    }

    fun beginRescue() {
        val program = state.value.activePrograms.firstOrNull() ?: return
        rescueStartedAtMillis = System.currentTimeMillis()
        rescueState.value = rescueFlow.begin(program)
    }

    fun tickRescuePause() {
        val current = rescueState.value
        if (current.secondsRemaining > 0) rescueState.value = rescueFlow.tick(current)
    }

    fun continueRescue() {
        rescueState.value = when (val current = rescueState.value) {
            else -> when (current.step) {
                com.deaddict.app.rescue.RescueStep.PAUSE -> rescueFlow.continueAfterPause(current)
                com.deaddict.app.rescue.RescueStep.MOTIVATION -> rescueFlow.acknowledgeMotivation(current)
                com.deaddict.app.rescue.RescueStep.INITIAL_URGE -> rescueFlow.continueToTrigger(current)
                else -> current
            }
        }
    }

    fun setRescueInitialUrge(value: Int) {
        rescueState.value = rescueFlow.setInitialUrge(rescueState.value, value)
    }

    fun chooseRescueTrigger(trigger: String) {
        rescueState.value = rescueFlow.chooseTrigger(rescueState.value, trigger)
    }

    fun chooseRescueAction(action: String) {
        rescueState.value = rescueFlow.chooseReplacement(rescueState.value, action)
    }

    fun setRescueFinalUrge(value: Int) {
        rescueState.value = rescueFlow.setFinalUrge(rescueState.value, value)
    }

    fun completeRescue() {
        val completed = rescueFlow.complete(rescueState.value)
        rescueState.value = completed
        val program = completed.program ?: return
        viewModelScope.launch {
            rescueRepository.record(
                NewRescueSession(
                    programId = program.id,
                    startedAtEpochMillis = rescueStartedAtMillis,
                    completedAtEpochMillis = System.currentTimeMillis(),
                    initialUrge = completed.initialUrge,
                    finalUrge = completed.finalUrge,
                    triggerKey = completed.triggerKey,
                    actionKeys = listOfNotNull(completed.selectedAction),
                    outcome = when {
                        completed.finalUrge < completed.initialUrge -> RescueOutcome.REDUCED
                        completed.finalUrge == completed.initialUrge -> RescueOutcome.SAME
                        else -> RescueOutcome.INCREASED
                    },
                ),
                SyncPolicy.LOCAL_ONLY,
            )
            refreshInsights(program.id.value)
        }
    }

    fun resetRescue() {
        rescueState.value = rescueFlow.reset()
    }

    fun setDailyNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferenceStore.setDailyEnabled(enabled)
            if (enabled) {
                notificationScheduler.scheduleDailyCheckIn()
            } else {
                notificationScheduler.cancelDailyCheckIn()
            }
        }
    }

    fun activateProgram(program: ProgramDefinition) {
        viewModelScope.launch {
            runCatching {
                programRepository.activate(program.id, SyncPolicy.LOCAL_ONLY)
            }.onSuccess {
                selectedProgramId.value = program.id.value
                selectedTab.value = AppTab.HOME
                message.value = if (state.value.activePrograms.isEmpty()) {
                    "Recovery track added."
                } else {
                    "Another recovery track was added. Its progress remains independent."
                }
            }.onFailure {
                message.value = "That recovery track could not be added. It may already be active."
            }
        }
    }

    fun recordTracking(program: ProgramDefinition, entry: TrackingEntry) {
        viewModelScope.launch {
            runCatching {
                val quantity = when (entry.kind) {
                    TrackingEventKind.QUANTITY,
                    TrackingEventKind.TIME,
                    -> entry.value
                    else -> null
                }
                val costMinorUnits = if (entry.kind == TrackingEventKind.COST) {
                    entry.value?.times(100)?.toLong()
                } else {
                    null
                }
                trackingRepository.record(
                    input = NewTrackingEvent(
                        programId = ProgramId.of(program.id.value),
                        kind = entry.kind,
                        quantity = quantity,
                        unit = when (entry.kind) {
                            TrackingEventKind.TIME -> "minutes"
                            TrackingEventKind.QUANTITY -> "units"
                            else -> null
                        },
                        costMinorUnits = costMinorUnits,
                        urgeIntensity = entry.urgeIntensity,
                        triggerKey = entry.triggerKey,
                        occurredAtEpochMillis = System.currentTimeMillis(),
                    ),
                    syncPolicy = SyncPolicy.LOCAL_ONLY,
                )
            }.onSuccess {
                message.value = when (entry.kind) {
                    TrackingEventKind.SLIP ->
                        "Slip recorded for ${program.displayName}. Your other recovery tracks are unchanged."
                    else -> "Check-in saved privately for ${program.displayName}."
                }
                refreshInsights(program.id.value)
            }.onFailure {
                message.value = "The entry could not be saved."
            }
        }
    }

    fun refreshInsights(programId: String? = null) {
        val program = programId
            ?.let(definitionsById::get)
            ?: state.value.activePrograms.firstOrNull()
            ?: return
        viewModelScope.launch {
            insights.value = withContext(Dispatchers.Default) {
                insightsRepository.sevenDays(program.id)
            }
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch { privacyPreferenceStore.setBiometricLock(enabled) }
    }

    fun setScreenProtection(enabled: Boolean) {
        viewModelScope.launch { privacyPreferenceStore.setScreenProtection(enabled) }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch { privacyPreferenceStore.setAnalytics(enabled) }
    }

    fun setUsageMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            privacyPreferenceStore.setUsageMonitoring(enabled)
            dailyUsage.value = if (enabled && digitalUsageRepository.hasUsageAccess()) {
                withContext(Dispatchers.Default) { digitalUsageRepository.estimateDay() }
            } else {
                null
            }
        }
    }

    fun deleteLocalRecoveryData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.clearAllTables() }
            notificationPreferenceStore.setDailyEnabled(false)
            notificationScheduler.cancelDailyCheckIn()
            selectedProgramId.value = null
            rescueState.value = rescueFlow.reset()
            insights.value = null
            message.value = "Local recovery data deleted."
        }
    }

    fun deleteAccount() {
        if (accountDeletionInProgress.value) return
        viewModelScope.launch {
            accountDeletionInProgress.value = true
            runCatching {
                accountDeletionCoordinator.deleteAccount()
            }.onSuccess { result ->
                selectedProgramId.value = null
                rescueState.value = rescueFlow.reset()
                insights.value = null
                selectedTab.value = AppTab.HOME
                message.value = if (result.localCleanupComplete) {
                    "Account and recovery data deleted."
                } else {
                    "Account deleted. Some local cleanup could not finish; clear DeAddict app storage before using it again."
                }
            }.onFailure {
                message.value = "Account deletion could not be confirmed. Check whether you can still sign in before retrying."
            }
            accountDeletionInProgress.value = false
        }
    }

    fun refreshBilling() = billingManager.refresh()

    fun purchasePlus(activity: Activity, offerToken: String) {
        billingManager.launchPurchase(activity, offerToken)
    }
}
