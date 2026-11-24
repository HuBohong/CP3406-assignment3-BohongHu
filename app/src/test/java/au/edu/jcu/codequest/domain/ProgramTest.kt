package au.edu.jcu.codequest.domain

import au.edu.jcu.codequest.domain.mission.Action
import au.edu.jcu.codequest.domain.mission.CommandBlock
import au.edu.jcu.codequest.domain.mission.Condition
import au.edu.jcu.codequest.domain.mission.IfBlock
import au.edu.jcu.codequest.domain.mission.PaletteBlock
import au.edu.jcu.codequest.domain.mission.Program
import au.edu.jcu.codequest.domain.mission.RepeatBlock
import au.edu.jcu.codequest.domain.mission.Sensor
import au.edu.jcu.codequest.domain.mission.Slot
import au.edu.jcu.codequest.domain.mission.WhileBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramTest {

    private val move = PaletteBlock.Act(Action.MOVE)
    private val sensor = Sensor.PATH_AHEAD

    @Test
    fun insertAtRoot_appendsBlocksWithFreshIds() {
        val program = Program()
            .insert(move, Slot.Root, sensor)
            .insert(PaletteBlock.Act(Action.TURN_LEFT), Slot.Root, sensor)

        assertEquals(listOf(1, 2), program.blocks.map { it.id })
        assertEquals(Action.TURN_LEFT, (program.blocks[1] as CommandBlock).action)
        assertEquals(2, program.lastInsertedId)
    }

    @Test
    fun insertIntoBody_nestsInsideTheContainer() {
        val withRepeat = Program().insert(PaletteBlock.Repeat, Slot.Root, sensor)
        val repeatId = withRepeat.lastInsertedId

        val program = withRepeat.insert(move, Slot.Body(repeatId), sensor)

        val repeat = program.blocks.single() as RepeatBlock
        assertEquals(Program.DEFAULT_REPEAT, repeat.times)
        assertEquals(Action.MOVE, (repeat.body.single() as CommandBlock).action)
        assertEquals(2, program.size)
    }

    @Test
    fun ifElse_hasAnElseSlotButPlainIfDoesNot() {
        val program = Program()
            .insert(PaletteBlock.IfElse, Slot.Root, sensor)
            .insert(PaletteBlock.If, Slot.Root, sensor)

        assertTrue(program.hasSlot(Slot.Else(1)))
        assertFalse(program.hasSlot(Slot.Else(2)))

        val updated = program.insert(move, Slot.Else(1), sensor)
        assertEquals(1, (updated.blocks[0] as IfBlock).elseBody?.size)
    }

    @Test
    fun insertIntoMissingOrCommandSlot_changesNothing() {
        val program = Program().insert(move, Slot.Root, sensor)

        assertSame(program, program.insert(move, Slot.Body(99), sensor))
        assertSame(program, program.insert(move, Slot.Body(1), sensor))
        assertSame(program, program.insert(move, Slot.Else(1), sensor))
    }

    @Test
    fun remove_takesNestedBlocksWithIt() {
        val program = Program()
            .insert(PaletteBlock.While, Slot.Root, sensor)
            .insert(move, Slot.Body(1), sensor)
            .insert(move, Slot.Root, sensor)

        val removed = program.remove(1)

        assertEquals(listOf(3), removed.blocks.map { it.id })
        assertNull(removed.find(2))
    }

    @Test
    fun remove_worksDeepInsideTheTree() {
        val program = Program()
            .insert(PaletteBlock.IfElse, Slot.Root, sensor)
            .insert(move, Slot.Body(1), sensor)
            .insert(move, Slot.Else(1), sensor)

        val removed = program.remove(3)

        val ifBlock = removed.blocks.single() as IfBlock
        assertEquals(1, ifBlock.body.size)
        assertEquals(0, ifBlock.elseBody?.size)
    }

    @Test
    fun repeatTimes_isKeptBetweenOneAndNine() {
        val program = Program().insert(PaletteBlock.Repeat, Slot.Root, sensor)

        assertEquals(9, (program.setRepeatTimes(1, 20).blocks[0] as RepeatBlock).times)
        assertEquals(1, (program.setRepeatTimes(1, 0).blocks[0] as RepeatBlock).times)
    }

    @Test
    fun setCondition_changesIfAndWhileOnly() {
        val program = Program()
            .insert(PaletteBlock.While, Slot.Root, sensor)
            .insert(move, Slot.Root, sensor)
        val negatedGoal = Condition(Sensor.AT_GOAL, negated = true)

        val updated = program.setCondition(1, negatedGoal).setCondition(2, negatedGoal)

        assertEquals(negatedGoal, (updated.blocks[0] as WhileBlock).condition)
        assertEquals("!atGoal()", negatedGoal.code)
        assertEquals(program.blocks[1], updated.blocks[1])
    }

    @Test
    fun programOf_continuesIdsAfterTheHighestOne() {
        val program = Program.of(
            RepeatBlock(1, 3, body = listOf(CommandBlock(7, Action.MOVE))),
        )

        assertEquals(8, program.nextId)
    }
}
