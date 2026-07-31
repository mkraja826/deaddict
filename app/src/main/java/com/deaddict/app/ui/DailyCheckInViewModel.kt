package com.deaddict.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deaddict.app.session.OwnerSessionStore
import com.deaddict.database.dao.DailyCheckInWithEntries
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.database.repository.DailyCheckInDraft
import com.deaddict.database.repository.LocalDailyCheckInRepository
import com.deaddict.database.repository.TrackCheckInDraft
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryTrackId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailyTrackCheckInUi(
    val recoveryTrackId: String,
    val outcome: TrackCheckInOutcome,
    val measuredValue: Double?,
    val unitKey: String?,
    val peakUrge: Int?,
    val privateNote: String?,
)

data class DailyCheckInUiState(
    val isLoading: Boolean = true,
    val localDateEpochDay: Long = LocalDate.now().toEpochDay(),
    val checkInId: String? = null,
    val updatedAtEpochMillis: Long = 0L,
    val mood: Int? = null,
    val stress: Int? = null,
    val energy: Int? = null,
    val sleepQuality: Int? = null,
    val entries: Map<String, DailyTrackCheckInUi> = emptyMap(),
    val isSaving: Boolean = false,
    val feedback: String? = null,
)

data class DailyTrackCheckInSubmission(
    val recoveryTrackId: String,
    val outcome: TrackCheckInOutcome,
    val measuredValue: Double? = null,
    val unitKey: String? = null,
    val peakUrge: Int? = null,
    val privateNote: String? = null,
)

data class DailyCheckInSubmission(
    val mood: Int? = null,
    val stress: Int? = null,
    val energy: Int? = null,
    val sleepQuality: Int? = null,
    val entries: List<DailyTrackCheckInSubmission>,
)

private data class OwnerDate(
    val ownerKey: OwnerKey?,
    val localDateEpochDay: Long,
)

private data class PersistedDailyCheckIn(
    val ownerDate: OwnerDate,
    val value: DailyCheckInWithEntries?,
)

@HiltViewModel
class DailyCheckInViewModel @Inject constructor(
    private val ownerSessionStore: OwnerSessionStore,
    private val repository: LocalDailyCheckInRepository,
) : ViewModel() {
    private val localDateEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    private val isSaving = MutableStateFlow(false)
    private val feedback = MutableStateFlow<String?>(null)

    private val ownerDate = combine(
        ownerSessionStore.state,
        localDateEpochDay,
    ) { session, date -> OwnerDate(session.ownerKey, date) }

    private val persisted = ownerDate.flatMapLatest { current ->
        val owner = current.ownerKey
        if (owner == null) {
            flowOf(PersistedDailyCheckIn(current, null))
        } else {
            repository.observeForDate(owner, current.localDateEpochDay)
                .map { PersistedDailyCheckIn(current, it) }
        }
    }

    val state: StateFlow<DailyCheckInUiState> = combine(
        persisted,
        isSaving,
        feedback,
    ) { persistedValue, saving, currentFeedback ->
        val record = persistedValue.value
        DailyCheckInUiState(
            isLoading = persistedValue.ownerDate.ownerKey == null,
            localDateEpochDay = persistedValue.ownerDate.localDateEpochDay,
            checkInId = record?.checkIn?.id,
            updatedAtEpochMillis = record?.checkIn?.updatedAtEpochMillis ?: 0L,
            mood = record?.checkIn?.mood,
            stress = record?.checkIn?.stress,
            energy = record?.checkIn?.energy,
            sleepQuality = record?.checkIn?.sleepQuality,
            entries = record?.entries.orEmpty().associate { entry ->
                entry.recoveryTrackId to DailyTrackCheckInUi(
                    recoveryTrackId = entry.recoveryTrackId,
                    outcome = entry.outcome,
                    measuredValue = entry.measuredValue,
                    unitKey = entry.unitKey,
                    peakUrge = entry.peakUrge,
                    privateNote = entry.privateNote,
                )
            },
            isSaving = saving,
            feedback = currentFeedback,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DailyCheckInUiState(),
    )

    fun refreshDate() {
        val today = LocalDate.now().toEpochDay()
        if (localDateEpochDay.value != today) {
            localDateEpochDay.value = today
            feedback.value = null
        }
    }

    fun save(submission: DailyCheckInSubmission) {
        if (isSaving.value || submission.entries.isEmpty()) return
        viewModelScope.launch {
            isSaving.value = true
            feedback.value = null
            try {
                val owner = checkNotNull(ownerSessionStore.state.first().ownerKey) {
                    "Recovery owner is unavailable"
                }
                repository.save(
                    DailyCheckInDraft(
                        ownerKey = owner,
                        localDateEpochDay = localDateEpochDay.value,
                        mood = submission.mood,
                        stress = submission.stress,
                        energy = submission.energy,
                        sleepQuality = submission.sleepQuality,
                        entries = submission.entries.map { entry ->
                            TrackCheckInDraft(
                                recoveryTrackId = RecoveryTrackId.parse(entry.recoveryTrackId),
                                outcome = entry.outcome,
                                measuredValue = entry.measuredValue,
                                unitKey = entry.unitKey,
                                peakUrge = entry.peakUrge,
                                privateNote = entry.privateNote,
                            )
                        },
                    ),
                )
                feedback.value = "Today’s check-in was saved privately."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                feedback.value = "Today’s check-in could not be saved. Your draft remains on screen."
            } finally {
                isSaving.value = false
            }
        }
    }
}
