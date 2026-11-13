package au.edu.jcu.codequest.data

import au.edu.jcu.codequest.data.local.TriviaDao
import au.edu.jcu.codequest.data.local.TriviaEntity
import au.edu.jcu.codequest.data.remote.TriviaApi
import au.edu.jcu.codequest.data.remote.TriviaQuestionDto
import au.edu.jcu.codequest.data.remote.TriviaResponseDto
import au.edu.jcu.codequest.data.repository.BonusRound
import au.edu.jcu.codequest.data.repository.NetworkTriviaRepository
import au.edu.jcu.codequest.domain.model.Difficulty
import au.edu.jcu.codequest.util.TimeProvider
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TriviaRepositoryTest {

    private val api = FakeTriviaApi()
    private val dao = FakeTriviaDao()

    private fun TestScope.createRepository() = NetworkTriviaRepository(
        triviaApi = api,
        triviaDao = dao,
        timeProvider = TimeProvider { 1_000L },
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private fun response(vararg questions: String) = TriviaResponseDto(
        responseCode = TriviaApi.RESPONSE_OK,
        results = questions.map { TriviaQuestionDto(it, "Right%20answer", listOf("Wrong%201", "Wrong%202", "C%2B%2B")) },
    )

    @Test
    fun success_decodesQuestionsAndCachesThem() = runTest {
        api.response = response("What%20is%20RAM%3F")

        val round = createRepository().loadBonusRound(Difficulty.EASY)

        assertTrue(round is BonusRound.Ready)
        round as BonusRound.Ready
        assertFalse(round.fromCache)
        val question = round.questions.single()
        assertEquals("What is RAM?", question.prompt)
        assertEquals("Right answer", question.options[question.correctIndex])
        assertTrue("C++" in question.options)
        assertEquals("easy", api.requestedDifficulties.single())
        assertEquals(1, dao.getByDifficulty("easy").size)
    }

    @Test
    fun networkError_fallsBackToTheCachedRound() = runTest {
        val repository = createRepository()
        api.response = response("Cached%20question")
        repository.loadBonusRound(Difficulty.EASY)

        api.error = IOException("offline")
        val round = repository.loadBonusRound(Difficulty.EASY)

        round as BonusRound.Ready
        assertTrue(round.fromCache)
        assertEquals("Cached question", round.questions.single().prompt)
    }

    @Test
    fun networkError_withNothingCached_isUnavailable() = runTest {
        api.error = IOException("offline")

        assertEquals(BonusRound.Unavailable, createRepository().loadBonusRound(Difficulty.MEDIUM))
    }

    @Test
    fun apiErrorCode_isTreatedLikeAFailedDownload() = runTest {
        api.response = TriviaResponseDto(responseCode = 5)

        assertEquals(BonusRound.Unavailable, createRepository().loadBonusRound(Difficulty.EASY))
    }

    @Test
    fun newDownload_replacesOnlyTheSameDifficulty() = runTest {
        val repository = createRepository()
        api.response = response("Easy%20one")
        repository.loadBonusRound(Difficulty.EASY)
        api.response = response("Hard%20one")
        repository.loadBonusRound(Difficulty.HARD)
        api.response = response("Easy%20two", "Easy%20three")
        repository.loadBonusRound(Difficulty.EASY)

        assertEquals(listOf("Easy two", "Easy three"), dao.getByDifficulty("easy").map { it.question })
        assertEquals(listOf("Hard one"), dao.getByDifficulty("hard").map { it.question })
    }

    @Test
    fun responseJson_parsesAndIgnoresUnknownFields() {
        val json = Json { ignoreUnknownKeys = true }
        val body = """
            {"response_code":0,"results":[{"type":"multiple","difficulty":"easy",
            "category":"Science%3A%20Computers","question":"Q%3F","correct_answer":"A",
            "incorrect_answers":["B","C","D"]}]}
        """.trimIndent()

        val parsed = json.decodeFromString<TriviaResponseDto>(body)

        assertEquals(0, parsed.responseCode)
        assertEquals("A", parsed.results.single().correctAnswer)
        assertEquals(3, parsed.results.single().incorrectAnswers.size)
    }
}

private class FakeTriviaApi : TriviaApi {
    var response: TriviaResponseDto? = null
    var error: Exception? = null
    val requestedDifficulties = mutableListOf<String>()

    override suspend fun getQuestions(
        amount: Int,
        difficulty: String,
        category: Int,
        type: String,
        encode: String,
    ): TriviaResponseDto {
        requestedDifficulties += difficulty
        error?.let { throw it }
        return checkNotNull(response) { "Test did not set a response" }
    }
}

/** Keeps rows in a list. The @Transaction method from the interface is inherited unchanged. */
private class FakeTriviaDao : TriviaDao {
    private val rows = mutableListOf<TriviaEntity>()
    private var nextId = 1L

    override suspend fun getByDifficulty(difficulty: String): List<TriviaEntity> =
        rows.filter { it.difficulty == difficulty }

    override suspend fun insertAll(questions: List<TriviaEntity>) {
        questions.forEach { rows += it.copy(id = nextId++) }
    }

    override suspend fun clearDifficulty(difficulty: String) {
        rows.removeAll { it.difficulty == difficulty }
    }
}
