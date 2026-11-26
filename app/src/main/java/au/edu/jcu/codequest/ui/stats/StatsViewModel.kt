package au.edu.jcu.codequest.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.codequest.data.repository.BlockCollectionRepository
import au.edu.jcu.codequest.data.repository.CurriculumRepository
import au.edu.jcu.codequest.data.repository.MissionRepository
import au.edu.jcu.codequest.data.repository.ProgressRepository
import au.edu.jcu.codequest.data.repository.SettingsRepository
import au.edu.jcu.codequest.domain.flight.FlightCatalog
import au.edu.jcu.codequest.domain.mission.BlockCollection
import au.edu.jcu.codequest.domain.mission.MissionCatalog
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
    /** The adventure beyond the lessons: planet missions, rocket flights and code blocks. */
    val missionsCompleted: Int = 0,
    val missionsTotal: Int = MissionCatalog.all.size,
    val flightsCompleted: Int = 0,
    val flightsTotal: Int = FlightCatalog.all.size,
    val blocksCollected: Int = 0,
    val blocksTotal: Int = BlockCollection.all.size,
)

/** Collects the learner's statistics from Room into one state object for the screen. */
@HiltViewModel
class StatsViewModel @Inject constructor(
    curriculumRepository: CurriculumRepository,
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    missionRepository: MissionRepository,
    blockCollectionRepository: BlockCollectionRepository,
) : ViewModel() {

    private val learningStats = combine(
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
    }

    val uiState: StateFlow<StatsUiState> = combine(
        learningStats,
        missionRepository.observeCompletedMissionIds(),
        blockCollectionRepository.observeCollected(),
    ) { stats, completedIds, blocks ->
        stats.copy(
            missionsCompleted = MissionCatalog.all.count { it.id in completedIds },
            flightsCompleted = FlightCatalog.all.count { it.id in completedIds },
            blocksCollected = blocks.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
