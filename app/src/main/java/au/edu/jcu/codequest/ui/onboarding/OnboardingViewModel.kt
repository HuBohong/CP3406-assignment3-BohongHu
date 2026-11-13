package au.edu.jcu.codequest.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.codequest.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Saves the learner's choice, then calls [onSaved] so the screen can move on. */
    fun choose(detailedHistory: Boolean, onSaved: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.completeOnboarding(detailedHistory)
            onSaved()
        }
    }
}
