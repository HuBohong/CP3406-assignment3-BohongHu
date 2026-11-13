package au.edu.jcu.codequest.data

import au.edu.jcu.codequest.data.seed.SeedData
import au.edu.jcu.codequest.domain.model.Answer
import au.edu.jcu.codequest.domain.model.QuestionType
import au.edu.jcu.codequest.domain.model.isCorrect
import au.edu.jcu.codequest.domain.model.scrambledLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the built-in course against authoring mistakes. */
class SeedDataTest {

    @Test
    fun ids_areUnique() {
        assertEquals(SeedData.lessons.size, SeedData.lessons.map { it.id }.toSet().size)
        assertEquals(SeedData.questions.size, SeedData.questions.map { it.id }.toSet().size)
    }

    @Test
    fun everyLesson_hasAtLeastFourQuestions() {
        val perLesson = SeedData.questions.groupBy { it.lessonId }

        SeedData.lessons.forEach { lesson ->
            assertTrue("${lesson.id} needs more questions", (perLesson[lesson.id]?.size ?: 0) >= 4)
        }
    }

    @Test
    fun everyQuestion_belongsToAKnownLesson() {
        val lessonIds = SeedData.lessons.map { it.id }.toSet()

        SeedData.questions.forEach { assertTrue("${it.id} has no lesson", it.lessonId in lessonIds) }
    }

    @Test
    fun choiceQuestions_haveAValidAnswerAndDistinctOptions() {
        SeedData.questions.filter { it.type == QuestionType.CHOICE }.forEach { question ->
            assertTrue("${question.id} needs options", question.options.size >= 2)
            assertTrue("${question.id} answer out of range", question.correctIndex in question.options.indices)
            assertEquals("${question.id} repeats an option", question.options.size, question.options.toSet().size)
            assertTrue(question.isCorrect(Answer.Choice(question.correctIndex)))
        }
    }

    @Test
    fun orderQuestions_startScrambledAndAreSolvable() {
        SeedData.questions.filter { it.type == QuestionType.ORDER }.forEach { question ->
            assertTrue("${question.id} needs at least three lines", question.options.size >= 3)
            assertFalse("${question.id} starts solved", question.isCorrect(Answer.Order(question.scrambledLines())))
            assertTrue(question.isCorrect(Answer.Order(question.options)))
        }
    }

    @Test
    fun everyQuestion_explainsItsAnswer() {
        SeedData.questions.forEach { assertTrue("${it.id} lacks an explanation", it.explanation.isNotBlank()) }
    }

    @Test
    fun lessons_areListedInPathOrder() {
        val sorted = SeedData.lessons.sortedWith(compareBy({ it.unitIndex }, { it.orderIndex }))

        assertEquals(sorted, SeedData.lessons)
    }
}
