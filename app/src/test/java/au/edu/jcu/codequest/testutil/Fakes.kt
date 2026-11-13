package au.edu.jcu.codequest.testutil

import au.edu.jcu.codequest.data.repository.BonusRound
import au.edu.jcu.codequest.data.repository.CurriculumRepository
import au.edu.jcu.codequest.data.repository.ProgressRepository
import au.edu.jcu.codequest.data.repository.SettingsRepository
import au.edu.jcu.codequest.data.repository.TriviaRepository
import au.edu.jcu.codequest.domain.model.AttemptRecord
import au.edu.jcu.codequest.domain.model.Difficulty
import au.edu.jcu.codequest.domain.model.Lesson
import au.edu.jcu.codequest.domain.model.LessonSummary
import au.edu.jcu.codequest.domain.model.Question
import au.edu.jcu.codequest.domain.model.QuestionType
import au.edu.jcu.codequest.domain.model.RecentSession
import au.edu.jcu.codequest.domain.model.SessionResult
import au.edu.jcu.codequest.domain.model.StatsOverview
import au.edu.jcu.codequest.domain.model.ThemeMode
import au.edu.jcu.codequest.domain.model.TopicAccuracy
import au.edu.jcu.codequest.domain.model.UserSettings
import au.edu.jcu.codequest.util.SoundPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// In-memory stand-ins for the repositories, so ViewModels can be tested on the JVM
// without a database, a network or an Android device.

fun testLesson(id: String, unitIndex: Int = 1, title: String = "Lesson $id") = Lesson(
    id = id,
    unitIndex = unitIndex,
    unitTitle = "Unit $unitIndex",
    orderIndex = 1,
    title = title,
    description = "About $id",
)

fun testSummary(id: String, unitIndex: Int = 1, timesCompleted: Int = 0, bestStars: Int = 0) =
    LessonSummary(testLesson(id, unitIndex), bestStars = bestStars, timesCompleted = timesCompleted)

/** A multiple-choice question whose correct option is index 1 ("right"). */
fun testChoiceQuestion(id: String, lessonId: String = "lesson", topic: String = "Topic") = Question(
    id = id,
    lessonId = lessonId,
    topic = topic,
    type = QuestionType.CHOICE,
    prompt = "Question $id",
    options = listOf("wrong", "right", "also wrong"),
    correctIndex = 1,
    explanation = "Explanation for $id",
)

fun testOrderQuestion(id: String, lessonId: String = "lesson") = Question(
    id = id,
    lessonId = lessonId,
    topic = "Ordering",
    type = QuestionType.ORDER,
    prompt = "Order $id",
    options = listOf("line one", "line two", "line three"),
    explanation = "Explanation for $id",
)

class FakeCurriculumRepository(
    lessons: List<LessonSummary> = emptyList(),
    private val questions: Map<String, List<Question>> = emptyMap(),
) : CurriculumRepository {
    val lessons = MutableStateFlow(lessons)

    override fun observeLessons(): Flow<List<LessonSummary>> = lessons

    override suspend fun getLesson(lessonId: String): Lesson? =
        lessons.value.firstOrNull { it.lesson.id == lessonId }?.lesson

    override suspend fun getQuestions(lessonId: String): List<Question> = questions[lessonId].orEmpty()
}

class FakeProgressRepository : ProgressRepository {
    data class Recorded(val result: SessionResult, val attempts: List<AttemptRecord>, val saveAttempts: Boolean)

    val overview = MutableStateFlow(StatsOverview())
    val topicAccuracy = MutableStateFlow(emptyList<TopicAccuracy>())
    val recentSessions = MutableStateFlow(emptyList<RecentSession>())
    val recorded = mutableListOf<Recorded>()
    var clearAttemptsCalls = 0
    var clearAllCalls = 0

    override fun observeOverview(): Flow<StatsOverview> = overview

    override fun observeTopicAccuracy(): Flow<List<TopicAccuracy>> = topicAccuracy

    override fun observeRecentSessions(limit: Int): Flow<List<RecentSession>> = recentSessions

    override suspend fun recordSession(result: SessionResult, attempts: List<AttemptRecord>, saveAttempts: Boolean) {
        recorded += Recorded(result, attempts, saveAttempts)
    }

    override suspend fun clearAttempts() {
        clearAttemptsCalls++
    }

    override suspend fun clearAll() {
        clearAllCalls++
    }
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)

    override val settings: Flow<UserSettings> = state

    override suspend fun completeOnboarding(detailedHistory: Boolean) {
        state.update { it.copy(onboardingDone = true, detailedHistory = detailedHistory) }
    }

    override suspend fun setDetailedHistory(enabled: Boolean) {
        state.update { it.copy(detailedHistory = enabled) }
    }

    override suspend fun setSoundOn(enabled: Boolean) {
        state.update { it.copy(soundOn = enabled) }
    }

    override suspend fun setDifficulty(difficulty: Difficulty) {
        state.update { it.copy(difficulty = difficulty) }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        state.update { it.copy(themeMode = themeMode) }
    }

    override suspend fun setUnlockAllLessons(enabled: Boolean) {
        state.update { it.copy(unlockAllLessons = enabled) }
    }
}

class FakeTriviaRepository(var round: BonusRound = BonusRound.Unavailable) : TriviaRepository {
    val requestedDifficulties = mutableListOf<Difficulty>()

    override suspend fun loadBonusRound(difficulty: Difficulty, amount: Int): BonusRound {
        requestedDifficulties += difficulty
        return round
    }
}

class FakeSoundPlayer : SoundPlayer {
    val played = mutableListOf<Boolean>()

    override fun play(correct: Boolean) {
        played += correct
    }
}
