package au.edu.jcu.codequest.ui.flight

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.codequest.data.repository.MissionRepository
import au.edu.jcu.codequest.domain.flight.Flight
import au.edu.jcu.codequest.domain.flight.FlightCatalog
import au.edu.jcu.codequest.domain.flight.FlightLevel
import au.edu.jcu.codequest.domain.flight.FlightOutcome
import au.edu.jcu.codequest.domain.flight.FlightResult
import au.edu.jcu.codequest.ui.navigation.Routes
import au.edu.jcu.codequest.util.FrameTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlightUiState(
    val level: FlightLevel? = null,
    val angle: Int = 45,
    val power: Int = 50,
    /** The launch being shown, from lift-off to where it ended. */
    val flight: FlightResult? = null,
    /** How many points of [flight]'s path are drawn so far. */
    val shownPoints: Int = 0,
    /** The launch before this one, drawn faintly so the learner can compare. */
    val previous: FlightResult? = null,
    val launches: Int = 0,
) {
    val isFlying: Boolean get() = flight != null && shownPoints < flight.path.size

    /** Set once the rocket has finished its flight. */
    val outcome: FlightOutcome? get() = flight?.takeIf { shownPoints >= it.path.size }?.outcome

    val canChangeValues: Boolean get() = flight == null
}

/**
 * Runs a rocket flight: the learner sets the two arguments of `launch(angle, power)`, watches
 * the parabola, and adjusts. The physics lives in the pure-Kotlin [Flight].
 */
@HiltViewModel
class FlightViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val missionRepository: MissionRepository,
    private val frameTimer: FrameTimer,
) : ViewModel() {

    private val flightId: String = checkNotNull(savedStateHandle.get<String>(Routes.ARG_FLIGHT_ID)) {
        "FlightViewModel needs a flightId navigation argument"
    }

    private val _uiState = MutableStateFlow(
        FlightCatalog.byId(flightId).let { level ->
            FlightUiState(level = level, angle = level?.startAngle ?: 45, power = level?.startPower ?: 50)
        },
    )
    val uiState: StateFlow<FlightUiState> = _uiState.asStateFlow()

    private var flyJob: Job? = null

    fun changeAngle(direction: Int) = _uiState.update {
        if (it.canChangeValues) it.copy(angle = Flight.nudge(it.angle, Flight.ANGLES, Flight.ANGLE_STEP, direction)) else it
    }

    fun changePower(direction: Int) = _uiState.update {
        if (it.canChangeValues) it.copy(power = Flight.nudge(it.power, Flight.POWERS, Flight.POWER_STEP, direction)) else it
    }

    /** Calls launch(angle, power) and animates the rocket along the result. */
    fun launch() {
        val state = _uiState.value
        val level = state.level ?: return
        if (!state.canChangeValues) return
        val result = Flight.simulate(level, state.angle, state.power)
        _uiState.update { it.copy(flight = result, shownPoints = 1, launches = it.launches + 1) }
        flyJob = viewModelScope.launch {
            while (_uiState.value.isFlying) {
                frameTimer.await(FRAME_MS)
                _uiState.update { it.copy(shownPoints = it.shownPoints + 1) }
            }
            missionRepository.recordAttempt(level.id, success = result.landed)
        }
    }

    /** Back to the dials, keeping the last flight on screen as a faint guide. */
    fun adjust() {
        flyJob?.cancel()
        flyJob = null
        _uiState.update { state ->
            state.copy(previous = state.flight ?: state.previous, flight = null, shownPoints = 0)
        }
    }

    private companion object {
        const val FRAME_MS = 24L
    }
}
