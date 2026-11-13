package au.edu.jcu.codequest

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.edu.jcu.codequest.data.local.AppDatabase
import au.edu.jcu.codequest.data.local.AttemptEntity
import au.edu.jcu.codequest.data.local.SessionEntity
import au.edu.jcu.codequest.data.local.toEntity
import au.edu.jcu.codequest.data.seed.SeedData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real Room queries against an in-memory database on a device or emulator. */
@RunWith(AndroidJUnit4::class)
class ProgressDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun session(lessonId: String, stars: Int, finishedAt: Long = 1_000L) = SessionEntity(
        lessonId = lessonId,
        lessonTitle = "Lesson $lessonId",
        total = 5,
        firstTryCorrect = stars + 2,
        stars = stars,
        xp = 60,
        finishedAt = finishedAt,
    )

    private suspend fun seedCurriculum() {
        database.curriculumDao().replaceCurriculum(
            lessons = SeedData.lessons.map { it.toEntity() },
            questions = SeedData.questions.mapIndexed { index, question -> question.toEntity(index) },
        )
    }

    @Test
    fun replayingALesson_keepsTheBestStarsAndCountsEachCompletion() = runTest {
        val dao = database.progressDao()

        dao.recordSession(session("basics-1", stars = 3), emptyList(), countsTowardsLesson = true)
        dao.recordSession(session("basics-1", stars = 1), emptyList(), countsTowardsLesson = true)

        val progress = dao.getProgress("basics-1")
        assertEquals(3, progress?.bestStars)
        assertEquals(2, progress?.timesCompleted)
        assertEquals(120, dao.observeTotalXp().first())
        assertEquals(2, dao.observeSessionCount().first())
    }

    @Test
    fun bonusRound_addsXpButNoLessonProgress() = runTest {
        val dao = database.progressDao()

        dao.recordSession(session("bonus", stars = 2), emptyList(), countsTowardsLesson = false)

        assertNull(dao.getProgress("bonus"))
        assertEquals(60, dao.observeTotalXp().first())
    }

    @Test
    fun topicAccuracy_isGroupedAndCounted() = runTest {
        val dao = database.progressDao()
        val attempts = listOf(
            AttemptEntity(questionId = "q1", topic = "Loops", correct = true, answeredAt = 1L),
            AttemptEntity(questionId = "q2", topic = "Loops", correct = false, answeredAt = 2L),
            AttemptEntity(questionId = "q3", topic = "Variables", correct = true, answeredAt = 3L),
        )

        dao.recordSession(session("flow-2", stars = 2), attempts, countsTowardsLesson = true)

        val rows = dao.observeTopicAccuracy().first()
        assertEquals(listOf("Loops", "Variables"), rows.map { it.topic })
        assertEquals(listOf(1, 1), rows.map { it.correctCount })
        assertEquals(listOf(2, 1), rows.map { it.totalCount })
    }

    @Test
    fun daysPractised_countsCalendarDaysNotSessions() = runTest {
        val dao = database.progressDao()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val noon = 12 * 60 * 60 * 1000L

        dao.recordSession(session("basics-1", stars = 3, finishedAt = noon), emptyList(), true)
        dao.recordSession(session("basics-2", stars = 3, finishedAt = noon + 60_000L), emptyList(), true)
        dao.recordSession(session("flow-1", stars = 3, finishedAt = noon + 5 * oneDayMs), emptyList(), true)

        assertEquals(2, dao.observeDaysPractised().first())
    }

    @Test
    fun lessonsWithProgress_joinsBestResultOntoThePath() = runTest {
        seedCurriculum()
        database.progressDao().recordSession(session("basics-1", stars = 2), emptyList(), countsTowardsLesson = true)

        val lessons = database.curriculumDao().observeLessonsWithProgress().first()

        assertEquals(SeedData.lessons.map { it.id }, lessons.map { it.lesson.id })
        assertEquals(2, lessons.first().bestStars)
        assertEquals(1, lessons.first().timesCompleted)
        assertEquals(0, lessons[1].timesCompleted)
    }

    @Test
    fun reseedingTheCourse_keepsLearnerProgress() = runTest {
        seedCurriculum()
        database.progressDao().recordSession(session("basics-1", stars = 3), emptyList(), countsTowardsLesson = true)

        seedCurriculum()

        assertEquals(SeedData.questions.size, database.curriculumDao().questionCount())
        assertEquals(3, database.progressDao().getProgress("basics-1")?.bestStars)
        assertEquals(5, database.curriculumDao().getQuestions("basics-1").size)
    }

    @Test
    fun clearAll_removesEverythingTheLearnerProduced() = runTest {
        val dao = database.progressDao()
        val attempts = listOf(AttemptEntity(questionId = "q1", topic = "Loops", correct = true, answeredAt = 1L))
        dao.recordSession(session("flow-2", stars = 2), attempts, countsTowardsLesson = true)

        dao.clearAll()

        assertNull(dao.getProgress("flow-2"))
        assertEquals(0, dao.observeSessionCount().first())
        assertTrue(dao.observeTopicAccuracy().first().isEmpty())
    }
}
