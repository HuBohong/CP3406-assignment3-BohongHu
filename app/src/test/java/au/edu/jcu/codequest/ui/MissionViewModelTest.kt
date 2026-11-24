package au.edu.jcu.codequest.ui

import androidx.lifecycle.SavedStateHandle
import au.edu.jcu.codequest.domain.mission.Action
import au.edu.jcu.codequest.domain.mission.CommandBlock
import au.edu.jcu.codequest.domain.mission.FailureReason
import au.edu.jcu.codequest.domain.mission.GridState
import au.edu.jcu.codequest.domain.mission.IfBlock
import au.edu.jcu.codequest.domain.mission.MissionCatalog
import au.edu.jcu.codequest.domain.mission.MissionOutcome
import au.edu.jcu.codequest.domain.mission.PaletteBlock
import au.edu.jcu.codequest.domain.mission.RepeatBlock
import au.edu.jcu.codequest.domain.mission.Sensor
import au.edu.jcu.codequest.domain.mission.Slot
import au.edu.jcu.codequest.testutil.FakeMissionRepository
import au.edu.jcu.codequest.testutil.MainDispatcherRule
import au.edu.jcu.codequest.ui.mission.MissionViewModel
import au.edu.jcu.codequest.ui.navigation.Routes
import au.edu.jcu.codequest.util.FrameTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MissionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val missions = FakeMissionRepository()
    private val waits = mutableListOf<Long>()

    /** Replays instantly but remembers how long each frame would have waited. */
    private val instantTimer = FrameTimer { waits += it }

    private fun createViewModel(missionId: String) = MissionViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_MISSION_ID to missionId)),
        missionRepository = missions,
        frameTimer = instantTimer,
    )

    private val move = PaletteBlock.Act(Action.MOVE)
    private val collect = PaletteBlock.Act(Action.COLLECT)

    @Test
    fun loadsTheMissionAndShowsItsStartingBoard() {
        val state = createViewModel("mission-2").uiState.value

        assertEquals(MissionCatalog.crystalCanyon, state.mission)
        assertTrue(state.boardState is GridState)
        assertTrue(state.isEditable)
        assertEquals(Slot.Root, state.target)
    }

    @Test
    fun unknownMission_hasNoMissionOrBoard() {
        val state = createViewModel("nope").uiState.value

        assertNull(state.mission)
        assertNull(state.boardState)
    }

    @Test
    fun addingALoop_makesItsBodyTheNextTarget() {
        val viewModel = createViewModel("mission-2")

        viewModel.addBlock(PaletteBlock.Repeat)
        viewModel.addBlock(move)

        val state = viewModel.uiState.value
        val repeat = state.program.blocks.single() as RepeatBlock
        assertEquals(Slot.Body(repeat.id), state.target)
        assertEquals(Action.MOVE, (repeat.body.single() as CommandBlock).action)
    }

    @Test
    fun newConditionsStartWithTheMissionsFirstSensor_andCycleThroughItsSensors() {
        val viewModel = createViewModel("mission-2")
        viewModel.addBlock(PaletteBlock.If)
        val id = viewModel.uiState.value.program.lastInsertedId

        assertEquals(Sensor.ON_CRYSTAL, (viewModel.uiState.value.program.find(id) as IfBlock).condition.sensor)

        viewModel.cycleSensor(id)
        assertEquals(Sensor.PATH_AHEAD, (viewModel.uiState.value.program.find(id) as IfBlock).condition.sensor)

        viewModel.cycleSensor(id)
        viewModel.toggleNegated(id)
        val condition = (viewModel.uiState.value.program.find(id) as IfBlock).condition
        assertEquals(Sensor.ON_CRYSTAL, condition.sensor)
        assertTrue(condition.negated)
    }

    @Test
    fun removingTheTargetBlock_sendsNewBlocksBackToTheTopLevel() {
        val viewModel = createViewModel("mission-2")
        viewModel.addBlock(PaletteBlock.Repeat)
        val repeatId = viewModel.uiState.value.program.lastInsertedId

        viewModel.removeBlock(repeatId)

        assertEquals(Slot.Root, viewModel.uiState.value.target)
        assertEquals(0, viewModel.uiState.value.program.size)
    }

    @Test
    fun repeatCountChangesStayInRange() {
        val viewModel = createViewModel("mission-2")
        viewModel.addBlock(PaletteBlock.Repeat)
        val id = viewModel.uiState.value.program.lastInsertedId

        repeat(20) { viewModel.changeRepeatTimes(id, 1) }

        assertEquals(9, (viewModel.uiState.value.program.find(id) as RepeatBlock).times)
    }

    @Test
    fun runningTheSolution_replaysEveryFrameAndRecordsTheWin() {
        val viewModel = createViewModel("mission-2")
        viewModel.addBlock(PaletteBlock.Repeat)
        val repeatId = viewModel.uiState.value.program.lastInsertedId
        repeat(4) { viewModel.changeRepeatTimes(repeatId, 1) } // 2 -> 6
        viewModel.addBlock(move)
        viewModel.addBlock(PaletteBlock.If)
        viewModel.addBlock(collect)

        viewModel.run()

        val state = viewModel.uiState.value
        assertEquals(MissionOutcome.Success, state.outcome)
        assertFalse(state.isEditable)
        assertEquals(state.playback!!.run.frames.size - 1, waits.size)
        assertEquals(listOf("mission-2" to true), missions.attempts)
    }

    @Test
    fun editsAreIgnoredWhileAReplayIsShowing_andAllowedAgainAfterwards() {
        val viewModel = createViewModel("mission-1")
        viewModel.addBlock(move)
        viewModel.run()

        viewModel.addBlock(move)
        assertEquals(1, viewModel.uiState.value.program.size)

        viewModel.backToEditing()
        viewModel.addBlock(move)
        assertEquals(2, viewModel.uiState.value.program.size)
        assertNull(viewModel.uiState.value.playback)
    }

    @Test
    fun failedRun_explainsWhyAndIsRecordedAsAnAttempt() {
        val viewModel = createViewModel("mission-1")
        viewModel.addBlock(PaletteBlock.Act(Action.TURN_LEFT))
        viewModel.addBlock(move)

        viewModel.run()

        assertEquals(MissionOutcome.Failure(FailureReason.LEFT_THE_MAP), viewModel.uiState.value.outcome)
        assertEquals(listOf("mission-1" to false), missions.attempts)
    }

    @Test
    fun stepping_showsOneFrameAtATimeAndRecordsOnlyAtTheEnd() {
        val viewModel = createViewModel("mission-1")
        viewModel.addBlock(move)
        viewModel.addBlock(move)

        viewModel.step()
        assertEquals(0, viewModel.uiState.value.playback?.index)
        assertNull(viewModel.uiState.value.runningBlockId)

        viewModel.step()
        assertEquals(1, viewModel.uiState.value.runningBlockId)
        assertTrue(missions.attempts.isEmpty())

        viewModel.step()
        assertEquals(2, viewModel.uiState.value.runningBlockId)
        assertEquals(MissionOutcome.Failure(FailureReason.CRYSTALS_LEFT), viewModel.uiState.value.outcome)
        assertEquals(listOf("mission-1" to false), missions.attempts)
    }
}
