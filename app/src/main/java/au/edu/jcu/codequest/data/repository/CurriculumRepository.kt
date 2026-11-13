package au.edu.jcu.codequest.data.repository

import au.edu.jcu.codequest.data.local.CurriculumDao
import au.edu.jcu.codequest.data.local.toDomain
import au.edu.jcu.codequest.data.local.toEntity
import au.edu.jcu.codequest.data.seed.SeedData
import au.edu.jcu.codequest.domain.model.Lesson
import au.edu.jcu.codequest.domain.model.LessonSummary
import au.edu.jcu.codequest.domain.model.Question
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The course: lessons in path order and the questions inside each lesson. */
interface CurriculumRepository {
    fun observeLessons(): Flow<List<LessonSummary>>
    suspend fun getLesson(lessonId: String): Lesson?
    suspend fun getQuestions(lessonId: String): List<Question>
}

class OfflineCurriculumRepository @Inject constructor(
    private val curriculumDao: CurriculumDao,
) : CurriculumRepository {

    private val seedMutex = Mutex()

    override fun observeLessons(): Flow<List<LessonSummary>> =
        curriculumDao.observeLessonsWithProgress()
            .onStart { seedIfNeeded() }
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getLesson(lessonId: String): Lesson? {
        seedIfNeeded()
        return curriculumDao.getLesson(lessonId)?.toDomain()
    }

    override suspend fun getQuestions(lessonId: String): List<Question> {
        seedIfNeeded()
        return curriculumDao.getQuestions(lessonId).map { it.toDomain() }
    }

    /**
     * Copies the bundled course into Room on first launch, and again after an app update that
     * changes the amount of content. Learner progress lives in separate tables and is untouched.
     */
    private suspend fun seedIfNeeded() = seedMutex.withLock {
        val upToDate = curriculumDao.lessonCount() == SeedData.lessons.size &&
            curriculumDao.questionCount() == SeedData.questions.size
        if (upToDate) return@withLock

        val questions = SeedData.questions
            .groupBy { it.lessonId }
            .flatMap { (_, inLesson) -> inLesson.mapIndexed { index, question -> question.toEntity(index) } }
        curriculumDao.replaceCurriculum(SeedData.lessons.map { it.toEntity() }, questions)
    }
}
