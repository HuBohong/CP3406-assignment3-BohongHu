package au.edu.jcu.codequest.ui

import au.edu.jcu.codequest.domain.flight.FlightCatalog
import au.edu.jcu.codequest.domain.mission.BlockCollection
import au.edu.jcu.codequest.domain.mission.MissionCatalog
import au.edu.jcu.codequest.domain.model.StatsOverview
import au.edu.jcu.codequest.domain.model.UserSettings
import au.edu.jcu.codequest.testutil.FakeBlockCollectionRepository
import au.edu.jcu.codequest.testutil.FakeCurriculumRepository
import au.edu.jcu.codequest.testutil.FakeMissionRepository
import au.edu.jcu.codequest.testutil.FakeProgressRepository
import au.edu.jcu.codequest.testutil.FakeSettingsRepository
import au.edu.jcu.codequest.testutil.MainDispatcherRule
import au.edu.jcu.codequest.testutil.testSummary
import au.edu.jcu.codequest.ui.stats.StatsUiState
import au.edu.jcu.codequest.ui.stats.StatsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val curriculum = FakeCurriculumRepository(
        listOf(
            testSummary("a", timesCompleted = 2, bestStars = 3),
            testSummary("b", timesCompleted = 1, bestStars = 1),
            testSummary("c"),
        ),
    )
    private val progress = FakeProgressRepository()
    private val settings = FakeSettingsRepository(UserSettings(onboardingDone = true))
    private val missions = FakeMissionRepository()
    private val blocks = FakeBlockCollectionRepository(emptySet())

    private val viewModel by lazy { StatsViewModel(curriculum, progress, settings, missions, blocks) }

    /** uiState only runs while it has a collector, so each test collects it in the background. */
    private fun TestScope.collectState(): () -> StatsUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return { viewModel.uiState.value }
    }

    @Test
    fun lessonTotals_comeFromTheCurriculumAndProgress() = runTest {
        progress.overview.value = StatsOverview(
            totalXp = 120,
            sessionsCompleted = 3,
            daysPractised = 2,
            firstTryCorrect = 9,
            questionsAnswered = 12,
        )
        val state = collectState()

        assertFalse(state().isLoading)
        assertEquals(2, state().completedLessons)
        assertEquals(3, state().totalLessons)
        assertEquals(120, state().totalXp)
        assertEquals(2, state().daysPractised)
        assertEquals(75, state().firstTryPercent)
    }

    @Test
    fun adventure_countsMissionsAndFlightsSeparately() = runTest {
        missions.completed.value = setOf(
            MissionCatalog.all[0].id,
            MissionCatalog.all[1].id,
            FlightCatalog.all[0].id,
        )
        val state = collectState()

        assertEquals(2, state().missionsCompleted)
        assertEquals(MissionCatalog.all.size, state().missionsTotal)
        assertEquals(1, state().flightsCompleted)
        assertEquals(FlightCatalog.all.size, state().flightsTotal)
    }

    @Test
    fun adventure_updatesWhenBlocksAreCollected() = runTest {
        val state = collectState()
        assertEquals(0, state().blocksCollected)
        assertEquals(BlockCollection.all.size, state().blocksTotal)

        blocks.collected.value = BlockCollection.rewardFor("basics-1").toSet()

        assertEquals(BlockCollection.rewardFor("basics-1").size, state().blocksCollected)
    }
}
