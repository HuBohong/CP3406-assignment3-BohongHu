package au.edu.jcu.codequest.domain

import au.edu.jcu.codequest.domain.mission.Action
import au.edu.jcu.codequest.domain.mission.Block
import au.edu.jcu.codequest.domain.mission.CommandBlock
import au.edu.jcu.codequest.domain.mission.Condition
import au.edu.jcu.codequest.domain.mission.FailureReason
import au.edu.jcu.codequest.domain.mission.IfBlock
import au.edu.jcu.codequest.domain.mission.MissionCatalog
import au.edu.jcu.codequest.domain.mission.MissionOutcome
import au.edu.jcu.codequest.domain.mission.MissionRunner
import au.edu.jcu.codequest.domain.mission.PaletteBlock
import au.edu.jcu.codequest.domain.mission.Program
import au.edu.jcu.codequest.domain.mission.RepeatBlock
import au.edu.jcu.codequest.domain.mission.Sensor
import au.edu.jcu.codequest.domain.mission.WhileBlock
import au.edu.jcu.codequest.domain.mission.bodyOrEmpty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Proves every mission is solvable with its own toolbox, and that common wrong ideas fail. */
class MissionCatalogTest {

    @Test
    fun everyReferenceSolutionCompletesItsMission() {
        MissionCatalog.all.forEach { mission ->
            val run = MissionRunner.run(mission, mission.referenceSolution)
            assertEquals("${mission.id} should be solvable", MissionOutcome.Success, run.outcome)
        }
    }

    @Test
    fun referenceSolutionsOnlyUseBlocksFromTheirOwnToolbox() {
        MissionCatalog.all.forEach { mission ->
            flatten(mission.referenceSolution.blocks).forEach { block ->
                val kind = when (block) {
                    is CommandBlock -> PaletteBlock.Act(block.action)
                    is RepeatBlock -> PaletteBlock.Repeat
                    is IfBlock -> if (block.elseBody != null) PaletteBlock.IfElse else PaletteBlock.If
                    is WhileBlock -> PaletteBlock.While
                }
                assertTrue("${mission.id} uses $kind", kind in mission.palette)
                if (block is IfBlock) assertTrue(block.condition.sensor in mission.sensors)
                if (block is WhileBlock) assertTrue(block.condition.sensor in mission.sensors)
            }
        }
    }

    @Test
    fun missionsCoverEachUnitOnce() {
        assertEquals(listOf(1, 2, 3, 4), MissionCatalog.all.map { it.unitIndex })
        assertEquals(MissionCatalog.all.size, MissionCatalog.all.map { it.id }.toSet().size)
        assertEquals(MissionCatalog.laserDrone, MissionCatalog.forUnit(3))
        assertEquals(MissionCatalog.crystalCanyon, MissionCatalog.byId("mission-2"))
    }

    @Test
    fun canyon_justMovingLeavesCrystalsBehind() {
        val program = Program.of(RepeatBlock(1, 6, body = listOf(CommandBlock(2, Action.MOVE))))

        val outcome = MissionRunner.run(MissionCatalog.crystalCanyon, program).outcome

        assertEquals(MissionOutcome.Failure(FailureReason.CRYSTALS_LEFT), outcome)
    }

    @Test
    fun drone_dodgingOnlyUpwardsIsNotEnough() {
        val program = Program.of(
            IfBlock(1, Condition(Sensor.LASER_HERE), body = listOf(CommandBlock(2, Action.MOVE_UP))),
        )

        val outcome = MissionRunner.run(MissionCatalog.laserDrone, program).outcome

        assertEquals(MissionOutcome.Failure(FailureReason.SHIELDS_GONE), outcome)
    }

    @Test
    fun drone_movingEveryTurnWithoutCheckingIsNotEnough() {
        val program = Program.of(CommandBlock(1, Action.MOVE_DOWN))

        val outcome = MissionRunner.run(MissionCatalog.laserDrone, program).outcome

        assertNotEquals(MissionOutcome.Success, outcome)
    }

    @Test
    fun longWayHome_goingStraightRunsOffTheMap() {
        val program = Program.of(
            WhileBlock(1, Condition(Sensor.AT_GOAL, negated = true), body = listOf(CommandBlock(2, Action.MOVE))),
        )

        val outcome = MissionRunner.run(MissionCatalog.longWayHome, program).outcome

        assertEquals(MissionOutcome.Failure(FailureReason.LEFT_THE_MAP), outcome)
    }

    @Test
    fun longWayHome_turningLeftInsteadSpinsInPlace() {
        val program = Program.of(
            WhileBlock(
                1,
                Condition(Sensor.AT_GOAL, negated = true),
                body = listOf(
                    IfBlock(
                        2,
                        Condition(Sensor.PATH_AHEAD),
                        body = listOf(CommandBlock(3, Action.MOVE)),
                        elseBody = listOf(CommandBlock(4, Action.TURN_LEFT)),
                    ),
                ),
            ),
        )

        val outcome = MissionRunner.run(MissionCatalog.longWayHome, program).outcome

        assertNotEquals(MissionOutcome.Success, outcome)
    }

    private fun flatten(blocks: List<Block>): List<Block> = blocks.flatMap { block ->
        listOf(block) + flatten(block.bodyOrEmpty()) + flatten((block as? IfBlock)?.elseBody.orEmpty())
    }
}
