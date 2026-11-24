package au.edu.jcu.codequest.ui

import au.edu.jcu.codequest.domain.model.StatsOverview
import au.edu.jcu.codequest.domain.model.UserSettings
import au.edu.jcu.codequest.testutil.FakeCurriculumRepository
import au.edu.jcu.codequest.testutil.FakeMissionRepository
import au.edu.jcu.codequest.testutil.FakeProgressRepository
import au.edu.jcu.codequest.testutil.FakeSettingsRepository
import au.edu.jcu.codequest.testutil.MainDispatcherRule
import au.edu.jcu.codequest.testutil.testSummary
import au.edu.jcu.codequest.ui.home.HomeUiState
import au.edu.jcu.codequest.ui.home.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val curriculum = FakeCurriculumRepository(
        lessons = listOf(
            testSummary("a", unitIndex = 1, timesCompleted = 1, bestStars = 2),
            testSummary("b", unitIndex = 1),
            testSummary("c", unitIndex = 2),
        ),
    )
    private val settings = FakeSettingsRepository()
    private val progress = FakeProgressRepository()
    private val missions = FakeMissionRepository()

    /** uiState only runs while it has a collector, so each test collects it in the background. */
    private fun TestScope.collectState(viewModel: HomeViewModel): () -> HomeUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return { viewModel.uiState.value }
    }

    private fun createViewModel() = HomeViewModel(curriculum, settings, progress, missions)

    @Test
    fun lessons_areGroupedIntoUnitsInPathOrder() = runTest {
        val state = collectState(createViewModel())()

        assertFalse(state.isLoading)
        assertEquals(listOf(1, 2), state.units.map { it.index })
        assertEquals(listOf("a", "b"), state.units[0].lessons.map { it.id })
        assertEquals(listOf(1, 2, 3), state.units.flatMap { it.lessons }.map { it.number })
    }

    @Test
    fun onlyTheLessonAfterACompletedOne_isOpen() = runTest {
        val lessons = collectState(createViewModel())().units.flatMap { it.lessons }

        assertEquals(listOf(false, false, true), lessons.map { it.isLocked })
        assertEquals(2, lessons.first().stars)
    }

    @Test
    fun unlockAllSetting_opensEveryLesson() = runTest {
        val state = collectState(createViewModel())
        assertTrue(state().units.flatMap { it.lessons }.any { it.isLocked })

        settings.state.value = UserSettings(unlockAllLessons = true)

        assertTrue(state().units.flatMap { it.lessons }.none { it.isLocked })
    }

    @Test
    fun unitMission_opensOnlyWhenEveryLessonInTheUnitIsDone() = runTest {
        val state = collectState(createViewModel())

        val unit1 = state().units[0].mission
        assertEquals("mission-1", unit1?.id)
        assertTrue("unit 1 still has lesson b to do", unit1?.isLocked == true)

        curriculum.lessons.value = listOf(
            testSummary("a", unitIndex = 1, timesCompleted = 1),
            testSummary("b", unitIndex = 1, timesCompleted = 1),
            testSummary("c", unitIndex = 2),
        )

        assertFalse(state().units[0].mission?.isLocked ?: true)
        assertTrue(state().units[1].mission?.isLocked ?: false)
    }

    @Test
    fun currentNode_isTheMissionOnceItsLessonsAreDone_thenMovesOn() = runTest {
        curriculum.lessons.value = listOf(
            testSummary("a", unitIndex = 1, timesCompleted = 1),
            testSummary("b", unitIndex = 1, timesCompleted = 1),
            testSummary("c", unitIndex = 2),
        )
        val state = collectState(createViewModel())
        assertEquals("mission-1", state().currentNodeId)

        missions.completed.value = setOf("mission-1")

        assertEquals("c", state().currentNodeId)
        assertTrue(state().units[0].mission?.isCompleted == true)
    }

    @Test
    fun bossMission_isMarkedAsABossFight() = runTest {
        curriculum.lessons.value = listOf(testSummary("x", unitIndex = 3))

        val mission = collectState(createViewModel())().units.single().mission

        assertTrue(mission?.isBoss == true)
    }

    @Test
    fun summary_countsCompletedLessonsAndPointsAtTheNextOne() = runTest {
        progress.overview.value = StatsOverview(totalXp = 70)

        val state = collectState(createViewModel())()

        assertEquals(1, state.completedLessons)
        assertEquals(3, state.totalLessons)
        assertEquals(70, state.totalXp)
        assertEquals("b", state.nextLesson?.id)
        assertEquals(1f / 3f, state.pathProgress, 0.001f)
    }
}
