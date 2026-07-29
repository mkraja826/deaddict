package com.deaddict.app.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deaddict.app.auth.AuthGateway
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
import com.deaddict.app.session.OwnerSessionState
import com.deaddict.app.session.OwnerSessionStore
import com.deaddict.app.usage.DailyUsageEstimate
import com.deaddict.app.usage.DigitalUsageRepository
import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.entity.RescueOutcome
import com.deaddict.database.entity.TrackingEventKind
import com.deaddict.database.repository.LocalRecoveryTrackRepository
import com.deaddict.database.repository.LocalRescueRepository
import com.deaddict.database.repository.LocalTrackingRepository
import com.deaddict.database.repository.NewRescueSession
import com.deaddict.database.repository.NewTrackingEvent
import com.deaddict.database.repository.RecoveryGoalDraft
import com.deaddict.database.repository.SyncPolicy
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrack
import com.deaddict.model.RecoveryTrackId
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.ProgramDefinition
import com.deaddict.programs.ProgramId
import com.deaddict.programs.ProgramRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

data class RecoveryTrackUi(
    val id: String,
    val program: ProgramDefinition,
    val displayAlias: String?,
    val role: RecoveryTrackRole,
    val status: RecoveryTrackStatus,
) {
    val title: String get() = displayAlias ?: program.displayName
    val isPrimary: Boolean get() = role == RecoveryTrackRole.PRIMARY
}

data class AppUiState(
    val isLoading: Boolean = true,
    val ownerKey: String? = null,
    val selectedTab: AppTab = AppTab.HOME,
    val availablePrograms: List<ProgramDefinition> = emptyList(),
    val recoveryTracks: List<RecoveryTrackUi> = emptyList(),
    val selectedRecoveryTrackId: String? = null,
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
    val selectedRecoveryTrack: RecoveryTrackUi?
        get() = recoveryTracks.firstOrNull { it.id == selectedRecoveryTrackId }

    val requiresOnboarding: Boolean
        get() = !isLoading && recoveryTracks.isEmpty()
}

data class TrackingEntry(
    val kind: TrackingEventKind,
    val value: Double? = null,
    val urgeIntensity: Int? = null,
    val triggerKey: String? = null,
)

private data class OwnerTracks(
    val session: OwnerSessionState,
    val tracks: List<RecoveryTrack>,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    registry: ProgramRegistry,
    private val authGateway: AuthGateway,
    private val ownerSessionStore: OwnerSessionStore,
    private val recoveryTrackRepository: LocalRecoveryTrackRepository,
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
    private val message = MutableStateFlow<String?>(null)
    private val usageAccessGranted = MutableStateFlow(false)
    private val dailyUsage = MutableStateFlow<DailyUsageEstimate?>(null)
    private val rescueFlow = RescueFlow()
    private val rescueState = MutableStateFlow(RescueFlowState())
    private val insights = MutableStateFlow<SevenDayInsights?>(null)
    private val accountDeletionInProgress = MutableStateFlow(false)
    private var rescueStartedAtMillis: Long = 0L
    private var rescueRecoveryTrackId: String? = null

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
    private val notificationPreferences = notificationPreferenceStore.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationPreferences(),
        )

    private val definitions = registry.all()
    private val definitionsById = definitions.associateBy { it.id.value }

    private val ownerTracks = ownerSessionStore.state
        .flatMapLatest { session ->
            val owner = session.ownerKey
            if (owner == null) {
                flowOf(OwnerTracks(session, emptyList()))
            } else {
                recoveryTrackRepository.observeOpen(owner)
                    .map { tracks -> OwnerTracks(session, tracks) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OwnerTracks(OwnerSessionState(), emptyList()),
        )

    private val primaryState = combine(
        ownerTracks,
        selectedTab,
        message,
    ) { current, tab, currentMessage ->
        val mapped = current.tracks.mapNotNull { track ->
            definitionsById[track.programId.value]?.let { definition ->
                RecoveryTrackUi(
                    id = track.id.value,
                    program = definition,
                    displayAlias = track.displayAlias,
                    role = track.role,
                    status = track.status,
                )
            }
        }
        val selectedId = resolveSelectedRecoveryTrackId(
            tracks = mapped.map { track ->
                RecoveryTrackSelectionCandidate(track.id, track.role, track.status)
            },
            requestedId = current.session.selectedRecoveryTrackId?.value,
        )
        val selected = mapped.firstOrNull { it.id == selectedId }

        // Temporary compatibility adapter for screens that still receive ProgramDefinition lists.
        val legacyPrograms = buildList {
            selected?.program?.let(::add)
            mapped.filterNot { it.id == selectedId }.mapTo(this) { it.program }
        }

        AppUiState(
            isLoading = current.session.ownerKey == null,
            ownerKey = current.session.ownerKey?.value,
            selectedTab = tab,
            availablePrograms = definitions,
            recoveryTracks = mapped,
            selectedRecoveryTrackId = selectedId,
            activePrograms = legacyPrograms,
            selectedProgramId = selected?.program?.id?.value,
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
        viewModelScope.launch { ownerSessionStore.resolve(authGateway) }
        viewModelScope.launch {
            ownerSessionStore.state
                .map { it.ownerKey }
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest(::reconcileOwnerScope)
        }
        viewModelScope.launch {
            ownerTracks.collectLatest { current ->
                val owner = current.session.ownerKey ?: return@collectLatest
                val candidates = current.tracks
                    .filter { definitionsById.containsKey(it.programId.value) }
                    .map { track ->
                        RecoveryTrackSelectionCandidate(
                            id = track.id.value,
                            role = track.role,
                            status = track.status,
                        )
                    }
                val resolved = resolveSelectedRecoveryTrackId(
                    tracks = candidates,
                    requestedId = current.session.selectedRecoveryTrackId?.value,
                )
                if (resolved != current.session.selectedRecoveryTrackId?.value) {
                    ownerSessionStore.selectRecoveryTrack(
                        ownerKey = owner,
                        trackId = resolved?.let(RecoveryTrackId::parse),
                    )
                }
            }
        }
        refreshDigitalUsage()
        billingManager.connect()
    }

    fun selectTab(tab: AppTab) {
        selectedTab.value = tab
        message.value = null
        if (tab == AppTab.INSIGHTS) refreshInsights()
    }

    fun selectRecoveryTrack(trackId: String) {
        val owner = currentOwner() ?: return
        val parsed = runCatching { RecoveryTrackId.parse(trackId) }.getOrNull() ?: return
        if (state.value.recoveryTracks.none { it.id == parsed.value }) return
        viewModelScope.launch {
            ownerSessionStore.selectRecoveryTrack(owner, parsed)
            rescueState.value = rescueFlow.reset()
            rescueRecoveryTrackId = null
            insights.value = null
            message.value = null
            if (selectedTab.value == AppTab.INSIGHTS) refreshInsights(parsed.value)
        }
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
        val track = state.value.selectedRecoveryTrack ?: return
        rescueRecoveryTrackId = track.id
        rescueStartedAtMillis = System.currentTimeMillis()
        rescueState.value = rescueFlow.begin(track.program)
    }

    fun tickRescuePause() {
        val current = rescueState.value
        if (current.secondsRemaining > 0) rescueState.value = rescueFlow.tick(current)
    }

    fun continueRescue() {
        val current = rescueState.value
        rescueState.value = when (current.step) {
            com.deaddict.app.rescue.RescueStep.PAUSE -> rescueFlow.continueAfterPause(current)
            com.deaddict.app.rescue.RescueStep.MOTIVATION -> rescueFlow.acknowledgeMotivation(current)
            com.deaddict.app.rescue.RescueStep.INITIAL_URGE -> rescueFlow.continueToTrigger(current)
            else -> current
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
        val trackId = rescueRecoveryTrackId ?: return
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
                syncPolicyFor(currentOwner()),
            )
            refreshInsights(trackId)
        }
    }

    fun resetRescue() {
        rescueRecoveryTrackId = null
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
        val owner = currentOwner() ?: return
        val hadTracks = state.value.recoveryTracks.isNotEmpty()
        viewModelScope.launch {
            runCatching {
                recoveryTrackRepository.create(
                    ownerKey = owner,
                    programId = program.id,
                    initialGoal = RecoveryGoalDraft(RecoveryGoalType.AWARENESS_ONLY),
                    syncPolicy = syncPolicyFor(owner),
                )
            }.onSuccess { trackId ->
                ownerSessionStore.selectRecoveryTrack(owner, trackId)
                selectedTab.value = AppTab.HOME
                message.value = if (hadTracks) {
                    "Another recovery track was added. Its progress remains independent."
                } else {
                    "Recovery track added."
                }
            }.onFailure {
                message.value = "That recovery track could not be added. It may already be open."
            }
        }
    }

    fun makePrimary(trackId: String) {
        mutateTrack(trackId, "Primary recovery track updated.") { owner, id ->
            recoveryTrackRepository.makePrimary(owner, id)
        }
    }

    fun pauseTrack(trackId: String) {
        mutateTrack(trackId, "Recovery track paused.") { owner, id ->
            recoveryTrackRepository.pause(owner, id)
        }
    }

    fun resumeTrack(trackId: String) {
        mutateTrack(trackId, "Recovery track resumed.") { owner, id ->
            recoveryTrackRepository.resume(owner, id)
        }
    }

    fun moveTrackToMaintenance(trackId: String) {
        mutateTrack(trackId, "Recovery track moved to maintenance.") { owner, id ->
            recoveryTrackRepository.enterMaintenance(owner, id)
        }
    }

    fun archiveTrack(trackId: String) {
        mutateTrack(trackId, "Recovery track archived. Its history is preserved.") { owner, id ->
            recoveryTrackRepository.archive(owner, id)
        }
    }

    fun recordTrackingSelected(entry: TrackingEntry) {
        val track = state.value.selectedRecoveryTrack ?: return
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
                        programId = ProgramId.of(track.program.id.value),
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
                    syncPolicy = syncPolicyFor(currentOwner()),
                )
            }.onSuccess {
                message.value = when (entry.kind) {
                    TrackingEventKind.SLIP ->
                        "Slip recorded for ${track.title}. Your other recovery tracks are unchanged."
                    else -> "Check-in saved privately for ${track.title}."
                }
                refreshInsights(track.id)
            }.onFailure {
                message.value = "The entry could not be saved."
            }
        }
    }

    fun refreshInsights(trackId: String? = null) {
        val track = trackId
            ?.let { requested -> state.value.recoveryTracks.firstOrNull { it.id == requested } }
            ?: state.value.selectedRecoveryTrack
            ?: return
        viewModelScope.launch {
            insights.value = withContext(Dispatchers.Default) {
                insightsRepository.sevenDays(track.program.id)
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
            val owner = currentOwner()
            withContext(Dispatchers.IO) { database.clearAllTables() }
            notificationPreferenceStore.setDailyEnabled(false)
            notificationScheduler.cancelDailyCheckIn()
            owner?.let { ownerSessionStore.selectRecoveryTrack(it, null) }
            rescueRecoveryTrackId = null
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
                rescueRecoveryTrackId = null
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

    private suspend fun reconcileOwnerScope(owner: OwnerKey) {
        val sources = buildList {
            add(OwnerKey.legacyLocal())
            if (owner.isAuthenticated) add(ownerSessionStore.guestOwnerKey())
        }.distinct().filterNot { it == owner }

        for (source in sources) {
            runCatching {
                recoveryTrackRepository.reconcileOwner(
                    from = source,
                    to = owner,
                    syncPolicy = syncPolicyFor(owner),
                )
            }.onFailure {
                message.value = "Some private recovery tracks could not be joined automatically. Their local data remains unchanged."
            }
        }
    }

    private fun mutateTrack(
        trackId: String,
        successMessage: String,
        action: suspend (OwnerKey, RecoveryTrackId) -> Any,
    ) {
        val owner = currentOwner() ?: return
        val parsed = runCatching { RecoveryTrackId.parse(trackId) }.getOrNull() ?: return
        if (state.value.recoveryTracks.none { it.id == parsed.value }) return
        viewModelScope.launch {
            runCatching { action(owner, parsed) }
                .onSuccess { message.value = successMessage }
                .onFailure { message.value = "That Recovery Track change could not be saved." }
        }
    }

    private fun currentOwner(): OwnerKey? =
        state.value.ownerKey?.let { stored -> runCatching { OwnerKey.parse(stored) }.getOrNull() }

    private fun syncPolicyFor(owner: OwnerKey?): SyncPolicy =
        if (owner?.isAuthenticated == true) SyncPolicy.CLOUD_ELIGIBLE else SyncPolicy.LOCAL_ONLY
}
