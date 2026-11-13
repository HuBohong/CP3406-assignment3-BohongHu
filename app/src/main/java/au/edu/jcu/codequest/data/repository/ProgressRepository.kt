package au.edu.jcu.codequest.data.repository

import au.edu.jcu.codequest.data.local.AttemptEntity
import au.edu.jcu.codequest.data.local.ProgressDao
import au.edu.jcu.codequest.data.local.SessionEntity
import au.edu.jcu.codequest.data.local.toDomain
import au.edu.jcu.codequest.domain.model.AttemptRecord
import au.edu.jcu.codequest.domain.model.RecentSession
import au.edu.jcu.codequest.domain.model.SessionResult
import au.edu.jcu.codequest.domain.model.StatsOverview
import au.edu.jcu.codequest.domain.model.TopicAccuracy
import au.edu.jcu.codequest.util.TimeProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** The learner's results and statistics. All of it stays in the on-device database. */
interface ProgressRepository {
    fun observeOverview(): Flow<StatsOverview>
    fun observeTopicAccuracy(): Flow<List<TopicAccuracy>>
    fun observeRecentSessions(limit: Int = DEFAULT_RECENT_LIMIT): Flow<List<RecentSession>>

    /**
     * Saves a finished session. Individual [attempts] are written only when [saveAttempts] is
     * true, i.e. when the learner has opted in to detailed answer history.
     */
    suspend fun recordSession(result: SessionResult, attempts: List<AttemptRecord>, saveAttempts: Boolean)

    /** Deletes the per-answer history but keeps lesson progress. */
    suspend fun clearAttempts()

    /** Deletes everything the learner has produced. */
    suspend fun clearAll()

    companion object {
        const val DEFAULT_RECENT_LIMIT = 10
    }
}

class OfflineProgressRepository @Inject constructor(
    private val progressDao: ProgressDao,
    private val timeProvider: TimeProvider,
) : ProgressRepository {

    override fun observeOverview(): Flow<StatsOverview> = combine(
        progressDao.observeTotalXp(),
        progressDao.observeSessionCount(),
        progressDao.observeDaysPractised(),
        progressDao.observeAnswerTotals(),
    ) { totalXp, sessions, days, totals ->
        StatsOverview(
            totalXp = totalXp,
            sessionsCompleted = sessions,
            daysPractised = days,
            firstTryCorrect = totals.correctCount,
            questionsAnswered = totals.totalCount,
        )
    }

    override fun observeTopicAccuracy(): Flow<List<TopicAccuracy>> =
        progressDao.observeTopicAccuracy().map { rows -> rows.map { it.toDomain() } }

    override fun observeRecentSessions(limit: Int): Flow<List<RecentSession>> =
        progressDao.observeRecentSessions(limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun recordSession(
        result: SessionResult,
        attempts: List<AttemptRecord>,
        saveAttempts: Boolean,
    ) {
        val now = timeProvider.now()
        val session = SessionEntity(
            lessonId = result.lessonId,
            lessonTitle = result.lessonTitle,
            total = result.total,
            firstTryCorrect = result.firstTryCorrect,
            stars = result.stars,
            xp = result.xp,
            finishedAt = now,
        )
        val attemptRows = if (saveAttempts) {
            attempts.map { AttemptEntity(questionId = it.questionId, topic = it.topic, correct = it.correct, answeredAt = now) }
        } else {
            emptyList()
        }
        progressDao.recordSession(session, attemptRows, countsTowardsLesson = !result.isBonus)
    }

    override suspend fun clearAttempts() = progressDao.clearAttempts()

    override suspend fun clearAll() = progressDao.clearAll()
}
