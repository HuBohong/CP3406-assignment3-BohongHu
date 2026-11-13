package au.edu.jcu.codequest.domain

import au.edu.jcu.codequest.domain.model.Lesson
import au.edu.jcu.codequest.domain.model.LessonSummary
import au.edu.jcu.codequest.domain.quiz.Progression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    private fun summary(id: String, timesCompleted: Int) = LessonSummary(
        lesson = Lesson(id, unitIndex = 1, unitTitle = "Unit", orderIndex = 1, title = id, description = ""),
        bestStars = if (timesCompleted > 0) 3 else 0,
        timesCompleted = timesCompleted,
    )

    @Test
    fun freshStart_onlyFirstLessonIsOpen() {
        val lessons = listOf(summary("a", 0), summary("b", 0), summary("c", 0))

        assertEquals(setOf("b", "c"), Progression.lockedLessonIds(lessons, unlockAll = false))
    }

    @Test
    fun completingALesson_opensTheNextOne() {
        val lessons = listOf(summary("a", 1), summary("b", 0), summary("c", 0))

        assertEquals(setOf("c"), Progression.lockedLessonIds(lessons, unlockAll = false))
    }

    @Test
    fun unlockAllSetting_opensEverything() {
        val lessons = listOf(summary("a", 0), summary("b", 0))

        assertTrue(Progression.lockedLessonIds(lessons, unlockAll = true).isEmpty())
    }

    @Test
    fun nextLesson_isFirstUnfinished() {
        val lessons = listOf(summary("a", 2), summary("b", 0), summary("c", 0))

        assertEquals("b", Progression.nextLesson(lessons)?.lesson?.id)
    }

    @Test
    fun nextLesson_wrapsToStartWhenEverythingIsDone() {
        val lessons = listOf(summary("a", 1), summary("b", 1))

        assertEquals("a", Progression.nextLesson(lessons)?.lesson?.id)
        assertNull(Progression.nextLesson(emptyList()))
    }
}
