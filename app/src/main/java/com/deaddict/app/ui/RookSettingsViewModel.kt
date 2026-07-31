package com.deaddict.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deaddict.app.coach.RookPreferenceStore
import com.deaddict.app.coach.RookTone
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class RookSettingsViewModel @Inject constructor(
    private val preferenceStore: RookPreferenceStore,
) : ViewModel() {
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { preferenceStore.setEnabled(enabled) }
    }

    fun setDefaultTone(tone: RookTone) {
        viewModelScope.launch { preferenceStore.setTone(tone) }
    }

    fun setAvatarVisible(visible: Boolean) {
        viewModelScope.launch { preferenceStore.setAvatarVisible(visible) }
    }

    fun setTrackTone(trackId: String, tone: RookTone?) {
        viewModelScope.launch { preferenceStore.setTrackTone(trackId, tone) }
    }

    fun clear() {
        viewModelScope.launch { preferenceStore.clear() }
    }
}
