package au.edu.jcu.codequest.data.repository

import au.edu.jcu.codequest.data.local.TriviaDao
import au.edu.jcu.codequest.data.local.TriviaEntity
import au.edu.jcu.codequest.data.local.toDomain
import au.edu.jcu.codequest.data.remote.TriviaApi
import au.edu.jcu.codequest.di.IoDispatcher
import au.edu.jcu.codequest.domain.model.Difficulty
import au.edu.jcu.codequest.domain.model.Question
import au.edu.jcu.codequest.domain.quiz.TriviaQuestions
import au.edu.jcu.codequest.util.TimeProvider
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Result of asking for a bonus round. */
sealed interface BonusRound {
    /** [fromCache] is true when the network failed and the last downloaded round is replayed. */
    data class Ready(val questions: List<Question>, val fromCache: Boolean) : BonusRound

    /** No connection and nothing cached yet. */
    data object Unavailable : BonusRound
}

/** Online bonus questions about computing, with an offline fallback. */
interface TriviaRepository {
    suspend fun loadBonusRound(difficulty: Difficulty, amount: Int = DEFAULT_AMOUNT): BonusRound

    companion object {
        const val DEFAULT_AMOUNT = 6
    }
}

class NetworkTriviaRepository @Inject constructor(
    private val triviaApi: TriviaApi,
    private val triviaDao: TriviaDao,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TriviaRepository {

    /**
     * Network first: a fresh round replaces the cached one for this difficulty. If the request
     * fails for any reason (offline, timeout, rate limit, bad data) the cached round is used.
     */
    override suspend fun loadBonusRound(difficulty: Difficulty, amount: Int): BonusRound =
        withContext(ioDispatcher) {
            try {
                val fresh = download(difficulty, amount)
                triviaDao.replaceForDifficulty(difficulty.apiValue, fresh)
                BonusRound.Ready(readCache(difficulty), fromCache = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val cached = readCache(difficulty)
                if (cached.isEmpty()) BonusRound.Unavailable else BonusRound.Ready(cached, fromCache = true)
            }
        }

    private suspend fun download(difficulty: Difficulty, amount: Int): List<TriviaEntity> {
        val response = triviaApi.getQuestions(amount = amount, difficulty = difficulty.apiValue)
        check(response.responseCode == TriviaApi.RESPONSE_OK && response.results.isNotEmpty()) {
            "Open Trivia DB returned code ${response.responseCode}"
        }
        val now = timeProvider.now()
        return response.results.map { dto ->
            TriviaEntity(
                difficulty = difficulty.apiValue,
                question = TriviaQuestions.decode(dto.question),
                correctAnswer = TriviaQuestions.decode(dto.correctAnswer),
                incorrectAnswers = dto.incorrectAnswers.map(TriviaQuestions::decode),
                fetchedAt = now,
            )
        }
    }

    private suspend fun readCache(difficulty: Difficulty): List<Question> =
        triviaDao.getByDifficulty(difficulty.apiValue).map { it.toDomain() }
}
