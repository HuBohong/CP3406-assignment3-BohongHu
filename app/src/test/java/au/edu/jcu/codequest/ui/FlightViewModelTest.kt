package au.edu.jcu.codequest.ui

import androidx.lifecycle.SavedStateHandle
import au.edu.jcu.codequest.domain.flight.FlightCatalog
import au.edu.jcu.codequest.domain.flight.FlightOutcome
import au.edu.jcu.codequest.testutil.FakeMissionRepository
import au.edu.jcu.codequest.testutil.MainDispatcherRule
import au.edu.jcu.codequest.ui.flight.FlightViewModel
import au.edu.jcu.codequest.ui.navigation.Routes
import au.edu.jcu.codequest.util.FrameTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FlightViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val missions = FakeMissionRepository()

    private fun createViewModel(id: String = "flight-1") = FlightViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_FLIGHT_ID to id)),
        missionRepository = missions,
        frameTimer = FrameTimer { },
    )

    @Test
    fun startsWithTheLevelsStartingValues() {
        val state = createViewModel().uiState.value

        assertEquals(FlightCatalog.toPlanetTwo, state.level)
        assertEquals(30, state.angle)
        assertEquals(40, state.power)
        assertTrue(state.canChangeValues)
    }

    @Test
    fun dials_moveInStepsAndStopAtTheEnds() {
        val viewModel = createViewModel()

        viewModel.changeAngle(+1)
        viewModel.changePower(-1)
        assertEquals(35, viewModel.uiState.value.angle)
        assertEquals(35, viewModel.uiState.value.power)

        repeat(30) { viewModel.changeAngle(+1) }
        assertEquals(80, viewModel.uiState.value.angle)
    }

    @Test
    fun aMissedLaunch_playsToTheEnd_andIsRecordedAsAFailedAttempt() {
        val viewModel = createViewModel()

        viewModel.launch()

        val state = viewModel.uiState.value
        assertEquals(FlightOutcome.TOO_SHORT, state.outcome)
        assertFalse(state.isFlying)
        assertFalse(state.canChangeValues)
        assertEquals(listOf("flight-1" to false), missions.attempts)
    }

    @Test
    fun adjusting_keepsTheLastFlightAsAGuide_andLandingCompletesTheFlight() {
        val viewModel = createViewModel()
        viewModel.launch()
        viewModel.adjust()

        val afterAdjust = viewModel.uiState.value
        assertNull(afterAdjust.flight)
        assertEquals(FlightOutcome.TOO_SHORT, afterAdjust.previous?.outcome)

        // 45 degrees at power 50 lands on planet 2's pad.
        repeat(3) { viewModel.changeAngle(+1) }
        repeat(2) { viewModel.changePower(+1) }
        viewModel.launch()

        assertEquals(FlightOutcome.LANDED, viewModel.uiState.value.outcome)
        assertTrue("flight-1" in missions.completed.value)
        assertEquals(2, viewModel.uiState.value.launches)
    }

    @Test
    fun dialsAreLocked_whileAFlightIsShowing() {
        val viewModel = createViewModel()
        viewModel.launch()

        viewModel.changeAngle(+1)

        assertEquals(30, viewModel.uiState.value.angle)
    }

    @Test
    fun unknownFlight_hasNoLevel_andCannotLaunch() {
        val viewModel = createViewModel("nope")
        viewModel.launch()

        assertNull(viewModel.uiState.value.level)
        assertNull(viewModel.uiState.value.flight)
    }
}
