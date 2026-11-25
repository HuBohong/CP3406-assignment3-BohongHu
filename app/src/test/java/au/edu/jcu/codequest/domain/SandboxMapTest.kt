package au.edu.jcu.codequest.domain

import au.edu.jcu.codequest.domain.mission.Action
import au.edu.jcu.codequest.domain.mission.BuildTool
import au.edu.jcu.codequest.domain.mission.Cell
import au.edu.jcu.codequest.domain.mission.CommandBlock
import au.edu.jcu.codequest.domain.mission.Heading
import au.edu.jcu.codequest.domain.mission.MissionOutcome
import au.edu.jcu.codequest.domain.mission.MissionRunner
import au.edu.jcu.codequest.domain.mission.Program
import au.edu.jcu.codequest.domain.mission.RepeatBlock
import au.edu.jcu.codequest.domain.mission.Sandbox
import au.edu.jcu.codequest.domain.mission.SandboxMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SandboxMapTest {

    private val map = SandboxMap.DEFAULT

    @Test
    fun paintingAFloorSquare_changesOnlyThatSquare() {
        val painted = map.paint(Cell(2, 1), BuildTool.CRATER)

        assertEquals('o', painted.symbolAt(Cell(2, 1)))
        assertEquals(map.rows.filterIndexed { y, _ -> y != 1 }, painted.rows.filterIndexed { y, _ -> y != 1 })
    }

    @Test
    fun startAndBeacon_moveInsteadOfMultiplying() {
        val moved = map.paint(Cell(3, 3), BuildTool.GOAL).paint(Cell(1, 1), BuildTool.START)

        assertEquals('G', moved.symbolAt(Cell(3, 3)))
        assertEquals('.', moved.symbolAt(Cell(5, 5)))
        assertEquals('B', moved.symbolAt(Cell(1, 1)))
        assertEquals('.', moved.symbolAt(Cell(0, 0)))
    }

    @Test
    fun startAndBeaconSquares_cannotBePaintedOver() {
        assertSame(map, map.paint(Cell(0, 0), BuildTool.ROCK))
        assertSame(map, map.paint(Cell(5, 5), BuildTool.CRYSTAL))
        assertSame(map, map.paint(Cell(9, 9), BuildTool.ROCK))
    }

    @Test
    fun startAndBeacon_cannotShareASquare() {
        assertSame(map, map.paint(Cell(5, 5), BuildTool.START))
        assertSame(map, map.paint(Cell(0, 0), BuildTool.GOAL))
    }

    @Test
    fun tappingTheStartWithTheStartTool_turnsByte() {
        val turned = map.paint(Cell(0, 0), BuildTool.START)

        assertEquals(Heading.SOUTH, turned.heading)
        assertEquals(Heading.SOUTH, turned.toLevel().heading)
    }

    @Test
    fun encodeAndDecode_roundTrip() {
        val custom = map.paint(Cell(1, 2), BuildTool.ROCK).paint(Cell(0, 0), BuildTool.START)

        assertEquals(custom, SandboxMap.decode(custom.encode()))
    }

    @Test
    fun decode_rejectsDamagedText() {
        assertNull(SandboxMap.decode(null))
        assertNull(SandboxMap.decode("E|B....."))
        assertNull(SandboxMap.decode("Q|B.....|......|......|......|......|.....G"))
        assertNull(SandboxMap.decode("E|BB....|......|......|......|......|.....G"))
    }

    @Test
    fun defaultPlanet_canBeSolved() {
        val program = Program.of(
            RepeatBlock(1, times = 2, body = listOf(CommandBlock(2, Action.MOVE))),
            CommandBlock(3, Action.TURN_RIGHT),
            RepeatBlock(4, times = 2, body = listOf(CommandBlock(5, Action.MOVE))),
            CommandBlock(6, Action.COLLECT),
            RepeatBlock(7, times = 3, body = listOf(CommandBlock(8, Action.MOVE))),
            CommandBlock(9, Action.TURN_LEFT),
            RepeatBlock(10, times = 3, body = listOf(CommandBlock(11, Action.MOVE))),
        )

        assertEquals(MissionOutcome.Success, MissionRunner.run(Sandbox.mission(map), program).outcome)
    }

    @Test
    fun stoppingShort_failsTheRun() {
        val program = Program.of(CommandBlock(1, Action.MOVE))

        assertTrue(MissionRunner.run(Sandbox.mission(map), program).outcome is MissionOutcome.Failure)
    }
}
