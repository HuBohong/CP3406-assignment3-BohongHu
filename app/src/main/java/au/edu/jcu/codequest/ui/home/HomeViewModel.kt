package au.edu.jcu.codequest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.codequest.data.repository.CurriculumRepository
import au.edu.jcu.codequest.data.repository.ProgressRepository
import au.edu.jcu.codequest.data.repository.SettingsRepository
import au.edu.jcu.codequest.domain.model.LessonSummary
import au.edu.jcu.codequest.domain.quiz.Progression
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LessonUi(
    val id: String,
    val number: Int,
    val title: String,
    val description: String,
    val stars: Int,
    val isCompleted: Boolean,
    val isLocked: Boolean,
)

data class UnitUi(
    val index: Int,
    val title: String,
    val lessons: List<LessonUi>,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val units: List<UnitUi> = emptyList(),
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    val totalXp: Int = 0,
    val nextLesson: LessonUi? = null,
) {
    val pathProgress: Float
        get() = if (totalLessons == 0) 0f else completedLessons.toFloat() / totalLessons
}

/** Builds the learning path for the landing page from lessons, progress and settings. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    curriculumRepository: CurriculumRepository,
    settingsRepository: SettingsRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        curriculumRepository.observeLessons(),
        settingsRepository.settings,
        progressRepository.observeOverview(),
    ) { lessons, settings, overview ->
        buildState(lessons, settings.unlockAllLessons, overview.totalXp)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    private fun buildState(lessons: List<LessonSummary>, unlockAll: Boolean, totalXp: Int): HomeUiState {
        val locked = Progression.lockedLessonIds(lessons, unlockAll)
        val lessonUis = lessons.mapIndexed { index, summary ->
            LessonUi(
                id = summary.lesson.id,
                number = index + 1,
                title = summary.lesson.title,
                description = summary.lesson.description,
                stars = summary.bestStars,
                isCompleted = summary.isCompleted,
                isLocked = summary.lesson.id in locked,
            )
        }
        val units = lessons.zip(lessonUis)
            .groupBy { (summary, _) -> summary.lesson.unitIndex }
            .map { (unitIndex, pairs) ->
                UnitUi(
                    index = unitIndex,
                    title = pairs.first().first.lesson.unitTitle,
                    lessons = pairs.map { it.second },
                )
            }
        val nextId = Progression.nextLesson(lessons)?.lesson?.id
        return HomeUiState(
            isLoading = false,
            units = units,
            completedLessons = lessons.count { it.isCompleted },
            totalLessons = lessons.size,
            totalXp = totalXp,
            nextLesson = lessonUis.firstOrNull { it.id == nextId },
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
