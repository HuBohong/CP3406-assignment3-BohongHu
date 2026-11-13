package au.edu.jcu.codequest.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.codequest.data.repository.ProgressRepository
import au.edu.jcu.codequest.data.repository.SettingsRepository
import au.edu.jcu.codequest.domain.model.Difficulty
import au.edu.jcu.codequest.domain.model.ThemeMode
import au.edu.jcu.codequest.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UserSettings())

    fun setSoundOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundOn(enabled) }
    }

    fun setDifficulty(difficulty: Difficulty) {
        viewModelScope.launch { settingsRepository.setDifficulty(difficulty) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(themeMode) }
    }

    fun setUnlockAllLessons(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUnlockAllLessons(enabled) }
    }

    /**
     * Withdrawing consent also deletes what was collected under it, so switching the
     * history off removes the stored answers instead of just hiding them.
     */
    fun setDetailedHistory(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDetailedHistory(enabled)
            if (!enabled) progressRepository.clearAttempts()
        }
    }

    /** Deletes all learning data. Preferences such as theme and sound are kept. */
    fun deleteProgress() {
        viewModelScope.launch { progressRepository.clearAll() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
