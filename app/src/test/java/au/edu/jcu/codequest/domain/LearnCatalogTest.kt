package au.edu.jcu.codequest.domain

import au.edu.jcu.codequest.domain.learn.FixTheBug
import au.edu.jcu.codequest.domain.learn.LearnCatalog
import au.edu.jcu.codequest.domain.learn.Playground
import au.edu.jcu.codequest.domain.learn.Scene
import au.edu.jcu.codequest.domain.learn.StepThrough
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the teaching cards: their simulations and their authoring. */
class LearnCatalogTest {

    /** Every combination of slot values, so each playground is checked exhaustively. */
    private fun allValueCombinations(card: Playground): List<List<String>> =
        card.slots.fold(listOf(emptyList())) { acc, slot -> acc.flatMap { prefix -> slot.options.map { prefix + it } } }

    @Test
    fun firstFiveLessonsHaveThreeCardsEach_andOtherLessonsNone() {
        assertEquals(listOf("basics-1", "basics-2", "flow-1", "flow-2", "fun-1"), LearnCatalog.lessonsWithCards)
        LearnCatalog.lessonsWithCards.forEach { assertEquals(3, LearnCatalog.forLesson(it).size) }
        assertTrue(LearnCatalog.forLesson("fun-2").isEmpty())
    }

    @Test
    fun everyPlaygroundRunsForEveryValue_withPlaceholdersMatchingSlots() {
        LearnCatalog.all.filterIsInstance<Playground>().forEach { card ->
            val used = card.code.flatMap { line -> Playground.PLACEHOLDER.findAll(line).map { it.groupValues[1].toInt() } }.toSet()
            assertEquals("${card.id} placeholders", card.slots.indices.toSet(), used)
            allValueCombinations(card).forEach { values ->
                val result = card.evaluate(values)
                result.highlightedLine?.let { assertTrue("${card.id} line", it in 1..card.code.size) }
                assertFalse("${card.id} renders", card.render(values).any { Playground.PLACEHOLDER.containsMatchIn(it) })
            }
        }
    }

    @Test
    fun everyPredictionMatchesWhatTheCodeReallyDoes() {
        assertEquals("75", LearnCatalog.fuelPlayground.evaluate(LearnCatalog.fuelPlayground.initialValues()).output.single())
        assertEquals("75", LearnCatalog.fuelPlayground.prediction!!.let { it.options[it.correctIndex] })

        val typesInitial = LearnCatalog.typesPlayground.evaluate(LearnCatalog.typesPlayground.initialValues()).scene as Scene.Types
        assertEquals("Double", typesInitial.values.first().type)

        val shields = LearnCatalog.shieldsPlayground.evaluate(LearnCatalog.shieldsPlayground.initialValues())
        assertEquals(3, shields.highlightedLine)
        assertEquals("raiseShields()", LearnCatalog.shieldsPlayground.code[2].trim())

        val launch = LearnCatalog.launchPlayground.evaluate(LearnCatalog.launchPlayground.initialValues()).scene as Scene.Launch
        assertEquals(5, launch.rockets.size)
    }

    @Test
    fun everyGoalCanBeReached() {
        LearnCatalog.all.filterIsInstance<Playground>().filter { it.goal != null }.forEach { card ->
            assertTrue("${card.id} goal reachable", allValueCombinations(card).any(card::isGoalMet))
            assertFalse("${card.id} goal not met at start", card.isGoalMet(card.initialValues()))
        }
    }

    @Test
    fun fuel_addsUpAndWarnsOnOverflow() {
        val card = LearnCatalog.fuelPlayground
        assertEquals(Scene.Fuel(100), card.evaluate(listOf("70", "30")).scene)
        assertTrue(card.isGoalMet(listOf("70", "30")))
        assertNotNull(card.evaluate(listOf("100", "60")).note)
    }

    @Test
    fun types_stringCannotBeMultiplied() {
        val result = LearnCatalog.typesPlayground.evaluate(listOf("\"3\""))
        assertNotNull(result.error)
        assertTrue(result.output.isEmpty())
        assertEquals(listOf("6"), LearnCatalog.typesPlayground.evaluate(listOf("3")).output)
    }

    @Test
    fun shields_equalCountsDoNotRaiseShields() {
        val result = LearnCatalog.shieldsPlayground.evaluate(listOf("5", "5"))
        assertEquals(Scene.Shields(5, raised = false), result.scene)
        assertEquals(5, result.highlightedLine)
    }

    @Test
    fun when_unknownPlanetFallsThroughToElse() {
        val result = LearnCatalog.whenPlayground.evaluate(listOf("\"Pluto\""))
        assertEquals(listOf("grey"), result.output)
        assertEquals(6, result.highlightedLine)
        assertEquals(4, LearnCatalog.whenPlayground.evaluate(listOf("\"Neptune\"")).highlightedLine)
    }

    @Test
    fun launch_rangesMatchKotlin() {
        assertEquals(listOf(1, 2, 3, 4, 5), LearnCatalog.launchedRockets("..", 5, 1))
        assertEquals(listOf(1, 2, 3, 4), LearnCatalog.launchedRockets("until", 5, 1))
        assertEquals(listOf(1, 3, 5), LearnCatalog.launchedRockets("..", 6, 2))
        assertTrue(LearnCatalog.launchedRockets("..", 0, 1).isEmpty())

        val empty = LearnCatalog.launchPlayground.evaluate(listOf("..", "0", "1"))
        assertNotNull(empty.note)
        assertTrue(empty.output.isEmpty())
    }

    @Test
    fun stepThroughs_pointAtRealLinesAndOutputOnlyGrows() {
        LearnCatalog.all.filterIsInstance<StepThrough>().forEach { card ->
            assertTrue(card.steps.isNotEmpty())
            card.steps.forEach { assertTrue("${card.id} line", it.line in 1..card.code.size) }
            card.steps.zipWithNext().forEach { (a, b) ->
                assertTrue("${card.id} output grows", b.output.size >= a.output.size && b.output.take(a.output.size) == a.output)
            }
        }
    }

    @Test
    fun countdown_printsThreeTwoOneThenLiftsOff() {
        val last = LearnCatalog.countdownTrace(3).last()
        assertEquals(listOf("3", "2", "1", "Liftoff!"), last.output)
        assertEquals(Scene.Countdown("Liftoff!", launched = true), last.scene)
        assertEquals("0", last.variables.single().value)
    }

    @Test
    fun countdownFromZero_skipsTheLoopBody() {
        val trace = LearnCatalog.countdownTrace(0)
        assertTrue(trace.none { it.line == 3 })
        assertEquals(listOf("Liftoff!"), trace.last().output)
    }

    @Test
    fun bugCards_pointAtARealLineAndChangeOnlyThatLine() {
        LearnCatalog.all.filterIsInstance<FixTheBug>().forEach { card ->
            assertTrue(card.buggyLine in 1..card.code.size)
            val changed = card.code.indices.filter { card.code[it] != card.fixedCode[it] }
            assertEquals("${card.id} fixes one line", listOf(card.buggyLine - 1), changed)
        }
        assertNull(LearnCatalog.watchFuel.steps.first().variables.firstOrNull { !it.isVal })
    }

    @Test
    fun hail_usesTheDefault_namedArgumentsInAnyOrder_andRejectsSwappedOnes() {
        val card = LearnCatalog.hailPlayground

        assertEquals(listOf("Hi Ada, crew of 1"), card.evaluate(listOf("\"Ada\"")).output)
        assertEquals(listOf("Hi Ada, crew of 4"), card.evaluate(listOf("n = 4, to = \"Ada\"")).output)
        val swapped = card.evaluate(listOf("4, \"Ada\""))
        assertEquals(3, swapped.highlightedLine)
        assertTrue(swapped.output.isEmpty())
        assertTrue(card.isGoalMet(listOf("\"Ada\", 4")))
        assertFalse(card.isGoalMet(listOf("\"Ada\"")))
    }

    @Test
    fun boost_jumpsIntoTheFunctionAndBack() {
        val lines = LearnCatalog.watchBoost.steps.map { it.line }

        assertEquals(listOf(1, 4, 2, 4, 5), lines)
        assertEquals(listOf("75"), LearnCatalog.watchBoost.steps.last().output)
        // The parameter only exists while the body runs.
        assertTrue(LearnCatalog.watchBoost.steps[2].variables.any { it.name == "fuel" })
        assertTrue(LearnCatalog.watchBoost.steps[3].variables.none { it.name == "fuel" })
    }
}
