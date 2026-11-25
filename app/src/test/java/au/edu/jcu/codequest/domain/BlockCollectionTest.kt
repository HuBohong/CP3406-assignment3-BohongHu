package au.edu.jcu.codequest.domain

import au.edu.jcu.codequest.data.seed.SeedData
import au.edu.jcu.codequest.domain.mission.Action
import au.edu.jcu.codequest.domain.mission.BlockCollection
import au.edu.jcu.codequest.domain.mission.MissionCatalog
import au.edu.jcu.codequest.domain.mission.PaletteBlock
import au.edu.jcu.codequest.domain.mission.Sandbox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockCollectionTest {

    private val lessons = SeedData.lessons

    @Test
    fun everyRewardComesFromARealLesson() {
        val ids = lessons.map { it.id }.toSet()
        assertTrue(BlockCollection.rewards.keys.all { it in ids })
    }

    @Test
    fun eachBlockIsCollectedOnlyOnce() {
        assertEquals(BlockCollection.all.size, BlockCollection.all.toSet().size)
    }

    @Test
    fun everyMissionCanBeBuiltFromBlocksCollectedBeforeIt() {
        MissionCatalog.all.forEach { mission ->
            val doneBefore = lessons.filter { it.unitIndex <= mission.unitIndex }.map { it.id }.toSet()
            val collected = BlockCollection.collected(doneBefore)
            val missing = mission.palette.filterNot { it in collected }
            assertTrue("${mission.title} needs $missing", missing.isEmpty())
        }
    }

    @Test
    fun collected_growsWithCompletedLessons() {
        assertTrue(BlockCollection.collected(emptySet()).isEmpty())
        assertEquals(
            setOf(PaletteBlock.Act(Action.MOVE), PaletteBlock.Act(Action.COLLECT)),
            BlockCollection.collected(setOf("basics-1")),
        )
        assertEquals(BlockCollection.all.toSet(), BlockCollection.collected(lessons.map { it.id }.toSet()))
    }

    @Test
    fun lessonFor_findsWhereABlockComesFrom() {
        assertEquals("flow-2", BlockCollection.lessonFor(PaletteBlock.Repeat))
        assertEquals("basics-1", BlockCollection.lessonFor(PaletteBlock.Act(Action.MOVE)))
    }

    @Test
    fun sandbox_offersOnlyBlocksThatWorkOnAGrid() {
        assertTrue(Sandbox.gridBlocks.none { it == PaletteBlock.Act(Action.MOVE_UP) || it == PaletteBlock.Act(Action.STAY) })
        assertEquals(8, Sandbox.gridBlocks.size)
    }
}
