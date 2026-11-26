package au.edu.jcu.codequest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.codequest.data.repository.BlockCollectionRepository
import au.edu.jcu.codequest.data.repository.CurriculumRepository
import au.edu.jcu.codequest.data.repository.MissionRepository
import au.edu.jcu.codequest.data.repository.ProgressRepository
import au.edu.jcu.codequest.data.repository.SettingsRepository
import au.edu.jcu.codequest.domain.flight.FlightCatalog
import au.edu.jcu.codequest.domain.mission.Action
import au.edu.jcu.codequest.domain.mission.BlockCollection
import au.edu.jcu.codequest.domain.mission.MissionCatalog
import au.edu.jcu.codequest.domain.mission.MissionKind
import au.edu.jcu.codequest.domain.mission.PaletteBlock
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

/**
 * The mission at the end of a unit, which opens once every lesson in the unit is done, or the
 * rocket flight to the next planet ([isFlight]), which opens once the mission is done.
 */
data class MissionNodeUi(
    val id: String,
    val title: String,
    val goal: String,
    val isBoss: Boolean,
    val isLocked: Boolean,
    val isCompleted: Boolean,
    val isFlight: Boolean = false,
)

data class UnitUi(
    val index: Int,
    val title: String,
    val lessons: List<LessonUi>,
    val mission: MissionNodeUi? = null,
    /** Optional flight from this unit's planet to the next one. */
    val flight: MissionNodeUi? = null,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val units: List<UnitUi> = emptyList(),
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    val totalXp: Int = 0,
    val nextLesson: LessonUi? = null,
    /** The lesson or mission the path points at next: the first one that is open and not done. */
    val currentNodeId: String? = null,
    val blocksCollected: Int = 0,
    val blocksTotal: Int = BlockCollection.all.size,
    /** The free-build planet opens once Byte has move() to work with. */
    val sandboxUnlocked: Boolean = false,
)

/** Builds the learning path for the landing page from lessons, missions, progress and settings. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    curriculumRepository: CurriculumRepository,
    settingsRepository: SettingsRepository,
    progressRepository: ProgressRepository,
    missionRepository: MissionRepository,
    blockCollectionRepository: BlockCollectionRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        curriculumRepository.observeLessons(),
        settingsRepository.settings,
        progressRepository.observeOverview(),
        missionRepository.observeCompletedMissionIds(),
        blockCollectionRepository.observeCollected(),
    ) { lessons, settings, overview, completedMissions, collected ->
        buildState(lessons, settings.unlockAllLessons, overview.totalXp, completedMissions).copy(
            blocksCollected = collected.size,
            sandboxUnlocked = PaletteBlock.Act(Action.MOVE) in collected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    private fun buildState(
        lessons: List<LessonSummary>,
        unlockAll: Boolean,
        totalXp: Int,
        completedMissions: Set<String>,
    ): HomeUiState {
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
                val unitLessons = pairs.map { it.second }
                val unitMission = MissionCatalog.forUnit(unitIndex)
                UnitUi(
                    index = unitIndex,
                    title = pairs.first().first.lesson.unitTitle,
                    lessons = unitLessons,
                    mission = MissionCatalog.forUnit(unitIndex)?.let { mission ->
                        MissionNodeUi(
                            id = mission.id,
                            title = mission.title,
                            goal = mission.goal,
                            isBoss = mission.kind == MissionKind.BOSS,
                            isLocked = !unlockAll && unitLessons.any { !it.isCompleted },
                            isCompleted = mission.id in completedMissions,
                        )
                    },
                    flight = FlightCatalog.fromUnit(unitIndex)?.let { flight ->
                        MissionNodeUi(
                            id = flight.id,
                            title = flight.title,
                            goal = flight.goal,
                            isBoss = false,
                            isLocked = !unlockAll && unitMission != null && unitMission.id !in completedMissions,
                            isCompleted = flight.id in completedMissions,
                            isFlight = true,
                        )
                    },
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
            currentNodeId = units.firstNotNullOfOrNull { unit ->
                unit.lessons.firstOrNull { !it.isCompleted && !it.isLocked }?.id
                    ?: unit.mission?.takeIf { !it.isCompleted && !it.isLocked }?.id
            },
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
