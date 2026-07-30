package com.deaddict.app.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deaddict.app.health.AppHealthMetric
import com.deaddict.app.health.AppHealthReporter
import com.deaddict.app.health.AppHealthThresholds
import com.deaddict.app.insights.InsightControls
import com.deaddict.app.insights.InsightPreferenceStore
import com.deaddict.app.insights.InsightWindow
import com.deaddict.app.insights.LocalInsightsRepository
import com.deaddict.app.insights.SevenDayInsights
import com.deaddict.app.session.OwnerSessionStore
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryTrackId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InsightsControlsUiState(
    val isLoading: Boolean = true,
    val selectedRecoveryTrackId: String? = null,
    val window: InsightWindow = InsightWindow.SEVEN_DAYS,
    val hiddenOtherTrackIds: Set<String> = emptySet(),
    val insights: SevenDayInsights? = null,
    val errorMessage: String? = null,
)

private data class InsightScope(
    val ownerKey: OwnerKey,
    val recoveryTrackId: RecoveryTrackId,
    val controls: InsightControls,
)

@HiltViewModel
class InsightsControlsViewModel @Inject constructor(
    ownerSessionStore: OwnerSessionStore,
    private val insightsRepository: LocalInsightsRepository,
    private val preferenceStore: InsightPreferenceStore,
    private val appHealthReporter: AppHealthReporter,
) : ViewModel() {
    private val mutableState = MutableStateFlow(InsightsControlsUiState())
    val state: StateFlow<InsightsControlsUiState> = mutableState

    private var currentScope: InsightScope? = null
    private var analyzedScopeKey: String? = null

    init {
        viewModelScope.launch {
            ownerSessionStore.state
                .flatMapLatest { session ->
                    val owner = session.ownerKey
                    val track = session.selectedRecoveryTrackId
                    if (owner == null || track == null) {
                        flowOf(null)
                    } else {
                        preferenceStore.observe(owner, track).map { controls ->
                            InsightScope(owner, track, controls)
                        }
                    }
                }
                .collectLatest { scope ->
                    currentScope = scope
                    if (scope == null) {
                        analyzedScopeKey = null
                        mutableState.value = InsightsControlsUiState(isLoading = false)
                        return@collectLatest
                    }

                    val nextAnalysisKey = scope.analysisKey()
                    val requiresAnalysis = analyzedScopeKey != nextAnalysisKey ||
                        mutableState.value.insights == null
                    mutableState.update { current ->
                        current.copy(
                            isLoading = requiresAnalysis,
                            selectedRecoveryTrackId = scope.recoveryTrackId.value,
                            window = scope.controls.window,
                            hiddenOtherTrackIds = scope.controls.hiddenOtherTrackIds,
                            insights = current.insights.takeUnless { analyzedScopeKey != nextAnalysisKey },
                            errorMessage = null,
                        )
                    }
                    if (requiresAnalysis) analyze(scope)
                }
        }
    }

    fun selectWindow(window: InsightWindow) {
        val scope = currentScope ?: return
        if (scope.controls.window == window) return
        viewModelScope.launch {
            preferenceStore.setWindow(scope.ownerKey, window)
        }
    }

    fun hideComparison(otherTrackId: String) {
        val scope = currentScope ?: return
        val other = runCatching { RecoveryTrackId.parse(otherTrackId) }.getOrNull() ?: return
        if (other == scope.recoveryTrackId) return
        viewModelScope.launch {
            preferenceStore.hideComparison(
                ownerKey = scope.ownerKey,
                selectedTrackId = scope.recoveryTrackId,
                otherTrackId = other,
            )
        }
    }

    fun restoreComparisons() {
        val scope = currentScope ?: return
        viewModelScope.launch {
            preferenceStore.restoreComparisons(scope.ownerKey, scope.recoveryTrackId)
        }
    }

    fun refresh() {
        val scope = currentScope ?: return
        viewModelScope.launch { analyze(scope, force = true) }
    }

    fun clearPreferences() {
        viewModelScope.launch {
            preferenceStore.clear()
            analyzedScopeKey = null
        }
    }

    private suspend fun analyze(scope: InsightScope, force: Boolean = false) {
        val key = scope.analysisKey()
        if (!force && analyzedScopeKey == key && mutableState.value.insights != null) return
        mutableState.update { it.copy(isLoading = true, errorMessage = null) }
        val startedAt = SystemClock.elapsedRealtime()
        try {
            val result = withContext(Dispatchers.Default) {
                insightsRepository.analyze(
                    recoveryTrackId = scope.recoveryTrackId,
                    window = scope.controls.window,
                )
            }
            if (currentScope?.analysisKey() != key) return
            appHealthReporter.record(AppHealthMetric.INSIGHTS_LOAD_SUCCESS)
            recordSlowLoadIfNeeded(startedAt)
            analyzedScopeKey = key
            mutableState.update {
                it.copy(
                    isLoading = false,
                    insights = result,
                    errorMessage = null,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            if (currentScope?.analysisKey() == key) {
                appHealthReporter.record(AppHealthMetric.INSIGHTS_LOAD_FAILURE)
                recordSlowLoadIfNeeded(startedAt)
                mutableState.update {
                    it.copy(
                        isLoading = false,
                        insights = null,
                        errorMessage = "Insights could not be refreshed.",
                    )
                }
            }
        }
    }

    private fun recordSlowLoadIfNeeded(startedAtElapsedRealtime: Long) {
        val duration = SystemClock.elapsedRealtime() - startedAtElapsedRealtime
        if (duration >= AppHealthThresholds.INSIGHTS_LOAD_SLOW_MILLIS) {
            appHealthReporter.record(AppHealthMetric.INSIGHTS_LOAD_SLOW)
        }
    }

    private fun InsightScope.analysisKey(): String =
        "${ownerKey.value}|${recoveryTrackId.value}|${controls.window.name}"
}
