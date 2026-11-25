package au.edu.jcu.codequest.ui.mission

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.codequest.data.repository.BlockCollectionRepository
import au.edu.jcu.codequest.data.repository.MissionRepository
import au.edu.jcu.codequest.data.repository.SandboxRepository
import au.edu.jcu.codequest.domain.mission.BuildTool
import au.edu.jcu.codequest.domain.mission.Cell
import au.edu.jcu.codequest.domain.mission.CommandBlock
import au.edu.jcu.codequest.domain.mission.Condition
import au.edu.jcu.codequest.domain.mission.Frame
import au.edu.jcu.codequest.domain.mission.IfBlock
import au.edu.jcu.codequest.domain.mission.Mission
import au.edu.jcu.codequest.domain.mission.MissionCatalog
import au.edu.jcu.codequest.domain.mission.MissionEvent
import au.edu.jcu.codequest.domain.mission.MissionOutcome
import au.edu.jcu.codequest.domain.mission.MissionRun
import au.edu.jcu.codequest.domain.mission.MissionRunner
import au.edu.jcu.codequest.domain.mission.MissionState
import au.edu.jcu.codequest.domain.mission.PaletteBlock
import au.edu.jcu.codequest.domain.mission.Program
import au.edu.jcu.codequest.domain.mission.RepeatBlock
import au.edu.jcu.codequest.domain.mission.Sandbox
import au.edu.jcu.codequest.domain.mission.SandboxMap
import au.edu.jcu.codequest.domain.mission.Sensor
import au.edu.jcu.codequest.domain.mission.Slot
import au.edu.jcu.codequest.domain.mission.WhileBlock
import au.edu.jcu.codequest.ui.navigation.Routes
import au.edu.jcu.codequest.util.FrameTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A run being replayed on the board. */
data class Playback(
    val run: MissionRun,
    val index: Int,
    val playing: Boolean,
) {
    val frame: Frame get() = run.frames[index]
    val finished: Boolean get() = index >= run.frames.lastIndex
}

data class MissionUiState(
    val mission: Mission? = null,
    val program: Program = Program(),
    /** Where tapped toolbox blocks are added. */
    val target: Slot = Slot.Root,
    /** Block whose edit controls (delete, count, condition) are showing. */
    val selectedBlockId: Int? = null,
    val playback: Playback? = null,
    val showHint: Boolean = false,
    /** Blocks the learner has collected; null until known, when every block is offered. */
    val collected: Set<PaletteBlock>? = null,
    /** Free-build only: the learner's own planet. */
    val sandboxMap: SandboxMap? = null,
    /** Free-build only: true while taps on the board change the planet instead of nothing. */
    val buildMode: Boolean = false,
    val buildTool: BuildTool = BuildTool.ROCK,
) {
    val isSandbox: Boolean get() = sandboxMap != null

    /** A toolbox block can be used once it has been collected in a lesson. */
    fun isCollected(item: PaletteBlock): Boolean = collected == null || item in collected

    /** What the board shows: the replay frame, or the starting layout while building. */
    val boardState: MissionState? get() = playback?.frame?.state ?: mission?.initialState

    val runningBlockId: Int? get() = playback?.frame?.activeBlockId

    val isEditable: Boolean get() = playback == null

    /** Set once a replay has reached its last frame and stopped. */
    val outcome: MissionOutcome? get() = playback?.takeIf { it.finished && !it.playing }?.run?.outcome
}

/**
 * Runs the mission screen: building the program with taps, replaying a run frame by frame,
 * and saving the result. All program logic lives in the pure-Kotlin [Program] and [MissionRunner].
 */
@HiltViewModel
class MissionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val missionRepository: MissionRepository,
    private val frameTimer: FrameTimer,
    private val blockCollectionRepository: BlockCollectionRepository,
    private val sandboxRepository: SandboxRepository,
) : ViewModel() {

    private val missionId: String = checkNotNull(savedStateHandle.get<String>(Routes.ARG_MISSION_ID)) {
        "MissionViewModel needs a missionId navigation argument"
    }
    private val isSandbox = missionId == Sandbox.ID

    private val _uiState = MutableStateFlow(
        if (isSandbox) {
            MissionUiState(mission = Sandbox.mission(SandboxMap.DEFAULT), sandboxMap = SandboxMap.DEFAULT, buildMode = true)
        } else {
            MissionUiState(mission = MissionCatalog.byId(missionId))
        },
    )
    val uiState: StateFlow<MissionUiState> = _uiState.asStateFlow()

    private var playJob: Job? = null

    init {
        viewModelScope.launch {
            blockCollectionRepository.observeCollected().collect { collected ->
                _uiState.update { it.copy(collected = collected) }
            }
        }
        if (isSandbox) {
            viewModelScope.launch {
                val saved = sandboxRepository.map.first()
                _uiState.update { it.copy(sandboxMap = saved, mission = Sandbox.mission(saved)) }
            }
        }
    }

    private val defaultSensor: Sensor
        get() = _uiState.value.mission?.sensors?.firstOrNull() ?: Sensor.PATH_AHEAD

    // ----- Building the program -----

    fun addBlock(item: PaletteBlock) = edit { state ->
        if (!state.isCollected(item)) return@edit state
        val program = state.program.insert(item, state.target, defaultSensor)
        if (program === state.program) return@edit state
        val newId = program.lastInsertedId
        // A new loop or if becomes the place where the next blocks go, which is what learners expect.
        val newTarget = if (item is PaletteBlock.Act) state.target else Slot.Body(newId)
        state.copy(program = program, target = newTarget, selectedBlockId = null)
    }

    fun selectTarget(slot: Slot) = edit { it.copy(target = slot, selectedBlockId = null) }

    fun toggleSelected(blockId: Int) = edit {
        it.copy(selectedBlockId = if (it.selectedBlockId == blockId) null else blockId)
    }

    fun removeBlock(blockId: Int) = edit { state ->
        val program = state.program.remove(blockId)
        state.copy(
            program = program,
            target = if (program.hasSlot(state.target)) state.target else Slot.Root,
            selectedBlockId = null,
        )
    }

    fun changeRepeatTimes(blockId: Int, delta: Int) = edit { state ->
        val block = state.program.find(blockId) as? RepeatBlock ?: return@edit state
        state.copy(program = state.program.setRepeatTimes(blockId, block.times + delta))
    }

    /** Switches an if or while to the mission's next sensor. */
    fun cycleSensor(blockId: Int) = edit { state ->
        val sensors = state.mission?.sensors.orEmpty()
        val condition = state.program.find(blockId).conditionOrNull() ?: return@edit state
        if (sensors.isEmpty()) return@edit state
        val next = sensors[(sensors.indexOf(condition.sensor) + 1).mod(sensors.size)]
        state.copy(program = state.program.setCondition(blockId, condition.copy(sensor = next)))
    }

    fun toggleNegated(blockId: Int) = edit { state ->
        val condition = state.program.find(blockId).conditionOrNull() ?: return@edit state
        state.copy(program = state.program.setCondition(blockId, condition.copy(negated = !condition.negated)))
    }

    fun clearProgram() = edit { it.copy(program = Program(), target = Slot.Root, selectedBlockId = null) }

    fun toggleHint() = _uiState.update { it.copy(showHint = !it.showHint) }

    // ----- Free-build planet -----

    /** Switches between designing the planet and writing the program. */
    fun toggleBuildMode() = edit { state ->
        if (state.isSandbox) state.copy(buildMode = !state.buildMode, selectedBlockId = null) else state
    }

    fun selectTool(tool: BuildTool) = edit { it.copy(buildTool = tool) }

    /** Uses the chosen tool on a square of the learner's planet and saves the result. */
    fun paint(cell: Cell) {
        val state = _uiState.value
        val map = state.sandboxMap ?: return
        if (!state.isEditable || !state.buildMode) return
        val painted = map.paint(cell, state.buildTool)
        if (painted == map) return
        _uiState.update { it.copy(sandboxMap = painted, mission = Sandbox.mission(painted)) }
        viewModelScope.launch { sandboxRepository.save(painted) }
    }

    // ----- Running -----

    /** Runs the whole program and replays it at a watchable speed. */
    fun run() {
        val mission = _uiState.value.mission ?: return
        val run = MissionRunner.run(mission, _uiState.value.program)
        playJob?.cancel()
        _uiState.update {
            it.copy(playback = Playback(run, index = 0, playing = true), selectedBlockId = null, buildMode = false)
        }
        playJob = viewModelScope.launch {
            while (true) {
                val playback = _uiState.value.playback ?: return@launch
                if (playback.finished) break
                frameTimer.await(delayFor(playback.run.frames[playback.index + 1]))
                _uiState.update { state -> state.playback?.let { state.copy(playback = it.copy(index = it.index + 1)) } ?: state }
            }
            _uiState.update { state -> state.playback?.let { state.copy(playback = it.copy(playing = false)) } ?: state }
            record(run)
        }
    }

    /** Shows the next frame only. Starts a run first if none is showing. */
    fun step() {
        val state = _uiState.value
        val mission = state.mission ?: return
        val playback = state.playback
        when {
            playback == null -> {
                val run = MissionRunner.run(mission, state.program)
                _uiState.update {
                    it.copy(playback = Playback(run, index = 0, playing = false), selectedBlockId = null, buildMode = false)
                }
                if (run.frames.size == 1) viewModelScope.launch { record(run) }
            }
            playback.playing || playback.finished -> Unit
            else -> {
                val next = playback.copy(index = playback.index + 1)
                _uiState.update { it.copy(playback = next) }
                if (next.finished) viewModelScope.launch { record(next.run) }
            }
        }
    }

    /** Stops any replay and goes back to editing, keeping the program. */
    fun backToEditing() {
        playJob?.cancel()
        playJob = null
        _uiState.update { it.copy(playback = null) }
    }

    private suspend fun record(run: MissionRun) {
        // The free-build planet is for play; it has no progress to save.
        if (isSandbox) return
        missionRepository.recordAttempt(missionId, success = run.outcome == MissionOutcome.Success)
    }

    /** Edits are only allowed while no replay is showing. */
    private inline fun edit(crossinline change: (MissionUiState) -> MissionUiState) {
        _uiState.update { state -> if (state.isEditable) change(state) else state }
    }

    private fun delayFor(frame: Frame): Long = when (frame.event) {
        null -> CHECK_FRAME_MS
        MissionEvent.CHARGING -> CHARGE_FRAME_MS
        MissionEvent.HIT, MissionEvent.DODGED -> FIRE_FRAME_MS
        else -> ACTION_FRAME_MS
    }

    private fun au.edu.jcu.codequest.domain.mission.Block?.conditionOrNull(): Condition? = when (this) {
        is IfBlock -> condition
        is WhileBlock -> condition
        is CommandBlock, is RepeatBlock, null -> null
    }

    private companion object {
        const val CHECK_FRAME_MS = 220L
        const val ACTION_FRAME_MS = 420L
        const val CHARGE_FRAME_MS = 520L
        const val FIRE_FRAME_MS = 620L
    }
}
