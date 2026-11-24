package au.edu.jcu.codequest.domain

import au.edu.jcu.codequest.domain.mission.Action
import au.edu.jcu.codequest.domain.mission.BossLevel
import au.edu.jcu.codequest.domain.mission.BossState
import au.edu.jcu.codequest.domain.mission.CommandBlock
import au.edu.jcu.codequest.domain.mission.Condition
import au.edu.jcu.codequest.domain.mission.FailureReason
import au.edu.jcu.codequest.domain.mission.GridLevel
import au.edu.jcu.codequest.domain.mission.GridState
import au.edu.jcu.codequest.domain.mission.Heading
import au.edu.jcu.codequest.domain.mission.IfBlock
import au.edu.jcu.codequest.domain.mission.Mission
import au.edu.jcu.codequest.domain.mission.MissionEvent
import au.edu.jcu.codequest.domain.mission.MissionKind
import au.edu.jcu.codequest.domain.mission.MissionOutcome
import au.edu.jcu.codequest.domain.mission.MissionRunner
import au.edu.jcu.codequest.domain.mission.Program
import au.edu.jcu.codequest.domain.mission.RepeatBlock
import au.edu.jcu.codequest.domain.mission.Sensor
import au.edu.jcu.codequest.domain.mission.WhileBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionEngineTest {

    private fun gridMission(vararg rows: String) = Mission(
        id = "test",
        unitIndex = 1,
        kind = MissionKind.GRID,
        title = "Test",
        goal = "",
        hint = "",
        palette = emptyList(),
        sensors = emptyList(),
        referenceSolution = Program(),
        grid = GridLevel.parse(rows.toList()),
    )

    private fun bossMission(vararg attacks: Set<Int>) = Mission(
        id = "boss",
        unitIndex = 1,
        kind = MissionKind.BOSS,
        title = "Boss",
        goal = "",
        hint = "",
        palette = emptyList(),
        sensors = emptyList(),
        referenceSolution = Program(),
        boss = BossLevel(attacks = attacks.toList()),
    )

    private fun commands(vararg actions: Action) =
        Program.of(*actions.mapIndexed { i, action -> CommandBlock(i + 1, action) }.toTypedArray())

    @Test
    fun parse_readsTilesCrystalsAndStart() {
        val level = GridLevel.parse(listOf("B.C", "#oG"), Heading.SOUTH)

        assertEquals(3, level.width)
        assertEquals(2, level.height)
        assertEquals(setOf(au.edu.jcu.codequest.domain.mission.Cell(2, 0)), level.crystals)
        assertEquals(Heading.SOUTH, level.heading)
    }

    @Test
    fun emptyProgram_failsWithoutRunning() {
        val run = MissionRunner.run(gridMission("BG"), Program())

        assertEquals(MissionOutcome.Failure(FailureReason.EMPTY_PROGRAM), run.outcome)
        assertEquals(1, run.frames.size)
    }

    @Test
    fun movingOntoTheGoalSucceeds() {
        val run = MissionRunner.run(gridMission("B.G"), commands(Action.MOVE, Action.MOVE))

        assertEquals(MissionOutcome.Success, run.outcome)
        assertEquals(3, run.frames.size)
        assertEquals(listOf(1, 2), run.frames.drop(1).map { it.activeBlockId })
    }

    @Test
    fun rocksCratersAndEdgesStopTheRun() {
        assertEquals(
            MissionOutcome.Failure(FailureReason.BUMPED_INTO_ROCK),
            MissionRunner.run(gridMission("B#G"), commands(Action.MOVE)).outcome,
        )
        assertEquals(
            MissionOutcome.Failure(FailureReason.FELL_INTO_CRATER),
            MissionRunner.run(gridMission("BoG"), commands(Action.MOVE, Action.MOVE)).outcome,
        )
        assertEquals(
            MissionOutcome.Failure(FailureReason.LEFT_THE_MAP),
            MissionRunner.run(gridMission("GB"), commands(Action.MOVE)).outcome,
        )
    }

    @Test
    fun fallingIntoACrater_stopsBeforeTheNextBlockRuns() {
        val run = MissionRunner.run(gridMission("BoG"), commands(Action.MOVE, Action.MOVE))

        assertEquals(MissionEvent.FELL, run.frames.last().event)
        assertEquals("start frame and the fall only", 2, run.frames.size)
    }

    @Test
    fun crystalsMustBeCollectedBeforeTheGoalCounts() {
        val mission = gridMission("BCG")

        assertEquals(
            MissionOutcome.Failure(FailureReason.CRYSTALS_LEFT),
            MissionRunner.run(mission, commands(Action.MOVE, Action.MOVE)).outcome,
        )
        assertEquals(
            MissionOutcome.Success,
            MissionRunner.run(mission, commands(Action.MOVE, Action.COLLECT, Action.MOVE)).outcome,
        )
    }

    @Test
    fun endingAwayFromTheGoalFails() {
        val run = MissionRunner.run(gridMission("B.G"), commands(Action.MOVE))

        assertEquals(MissionOutcome.Failure(FailureReason.NOT_AT_GOAL), run.outcome)
    }

    @Test
    fun turningChangesWhereMoveGoes() {
        val run = MissionRunner.run(
            gridMission("B#", ".G"),
            commands(Action.TURN_RIGHT, Action.MOVE, Action.TURN_LEFT, Action.MOVE),
        )

        assertEquals(MissionOutcome.Success, run.outcome)
        assertEquals(Heading.EAST, (run.frames.last().state as GridState).heading)
    }

    @Test
    fun repeatRunsItsBodyTheGivenNumberOfTimes() {
        val program = Program.of(RepeatBlock(1, times = 3, body = listOf(CommandBlock(2, Action.MOVE))))

        val run = MissionRunner.run(gridMission("B..G"), program)

        assertEquals(MissionOutcome.Success, run.outcome)
        assertEquals(3, run.frames.count { it.activeBlockId == 2 })
    }

    @Test
    fun whileLoopThatNeverEnds_isStoppedByTheStepLimit() {
        val program = Program.of(
            WhileBlock(1, Condition(Sensor.AT_GOAL, negated = true), body = listOf(CommandBlock(2, Action.TURN_LEFT))),
        )

        val run = MissionRunner.run(gridMission("B.G"), program)

        assertEquals(MissionOutcome.Failure(FailureReason.TOO_MANY_STEPS), run.outcome)
    }

    @Test
    fun boss_programRunsEveryTurnAndTheFirstMoveEndsTheTurn() {
        // Two moves in a row: only the first one happens each turn.
        val program = commands(Action.MOVE_UP, Action.MOVE_UP)

        val run = MissionRunner.run(bossMission(setOf(1), setOf(1)), program)

        val states = run.frames.map { it.state as BossState }
        assertEquals(0, states.last().byteLane)
        assertEquals(2, run.frames.count { it.event == MissionEvent.DODGED })
        assertEquals(MissionOutcome.Success, run.outcome)
    }

    @Test
    fun boss_movingUpFromTheTopLaneIsBlocked() {
        val program = commands(Action.MOVE_UP)

        val run = MissionRunner.run(bossMission(setOf(0), setOf(0), setOf(0)), program)

        assertTrue(run.frames.any { it.event == MissionEvent.BLOCKED_AT_EDGE })
        assertEquals(MissionOutcome.Failure(FailureReason.SHIELDS_GONE), run.outcome)
        assertEquals(0, (run.frames.last().state as BossState).shields)
    }

    @Test
    fun boss_sensorsReadTheChargingAttack() {
        val dodge = Program.of(
            IfBlock(
                1,
                Condition(Sensor.LASER_HERE),
                body = listOf(
                    IfBlock(
                        2,
                        Condition(Sensor.SAFE_BELOW),
                        body = listOf(CommandBlock(3, Action.MOVE_DOWN)),
                        elseBody = listOf(CommandBlock(4, Action.MOVE_UP)),
                    ),
                ),
            ),
        )

        // Turn 1: dodge down to lane 2. Turn 2: lane 2 is the bottom, so the else branch moves up.
        val run = MissionRunner.run(bossMission(setOf(1), setOf(2)), dodge)

        assertEquals(MissionOutcome.Success, run.outcome)
        assertEquals(3, (run.frames.last().state as BossState).shields)
    }

    @Test(expected = IllegalArgumentException::class)
    fun boss_attackThatCoversEveryLaneIsRejected() {
        BossLevel(attacks = listOf(setOf(0, 1, 2)))
    }
}
