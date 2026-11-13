package au.edu.jcu.codequest.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.codequest.data.repository.CurriculumRepository
import au.edu.jcu.codequest.data.repository.ProgressRepository
import au.edu.jcu.codequest.data.repository.SettingsRepository
import au.edu.jcu.codequest.domain.model.RecentSession
import au.edu.jcu.codequest.domain.model.TopicAccuracy
import au.edu.jcu.codequest.domain.quiz.Scoring
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val isLoading: Boolean = true,
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    val totalXp: Int = 0,
    val daysPractised: Int = 0,
    val sessionsCompleted: Int = 0,
    val firstTryPercent: Int = 0,
    /** False when the learner declined detailed history; topic accuracy is then unavailable. */
    val detailedHistory: Boolean = false,
    val topicAccuracy: List<TopicAccuracy> = emptyList(),
    val recentSessions: List<RecentSession> = emptyList(),
)

/** Collects the learner's statistics from Room into one state object for the screen. */
@HiltViewModel
class StatsViewModel @Inject constructor(
    curriculumRepository: CurriculumRepository,
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        curriculumRepository.observeLessons(),
        progressRepository.observeOverview(),
        progressRepository.observeTopicAccuracy(),
        progressRepository.observeRecentSessions(),
        settingsRepository.settings,
    ) { lessons, overview, topics, recent, settings ->
        StatsUiState(
            isLoading = false,
            completedLessons = lessons.count { it.isCompleted },
            totalLessons = lessons.size,
            totalXp = overview.totalXp,
            daysPractised = overview.daysPractised,
            sessionsCompleted = overview.sessionsCompleted,
            firstTryPercent = Scoring.accuracyPercent(overview.firstTryCorrect, overview.questionsAnswered),
            detailedHistory = settings.detailedHistory,
            topicAccuracy = topics,
            recentSessions = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
