package au.edu.jcu.codequest.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Everything the learner produces: finished sessions, best results and optional answer history. */
@Dao
interface ProgressDao {

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun getProgress(lessonId: String): LessonProgressEntity?

    @Upsert
    suspend fun upsertProgress(progress: LessonProgressEntity)

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Insert
    suspend fun insertAttempts(attempts: List<AttemptEntity>)

    /**
     * Saves a finished session atomically. Progress only ever improves: [LessonProgressEntity.bestStars]
     * keeps the highest rating, so replaying a lesson can never lower it.
     */
    @Transaction
    suspend fun recordSession(
        session: SessionEntity,
        attempts: List<AttemptEntity>,
        countsTowardsLesson: Boolean,
    ) {
        insertSession(session)
        if (attempts.isNotEmpty()) insertAttempts(attempts)
        if (countsTowardsLesson) {
            val existing = getProgress(session.lessonId)
            upsertProgress(
                LessonProgressEntity(
                    lessonId = session.lessonId,
                    bestStars = maxOf(existing?.bestStars ?: 0, session.stars),
                    timesCompleted = (existing?.timesCompleted ?: 0) + 1,
                    lastCompletedAt = session.finishedAt,
                ),
            )
        }
    }

    @Query("SELECT COALESCE(SUM(xp), 0) FROM sessions")
    fun observeTotalXp(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sessions")
    fun observeSessionCount(): Flow<Int>

    /** Number of different calendar days (device time zone) with at least one finished session. */
    @Query(
        """
        SELECT COUNT(DISTINCT strftime('%Y-%m-%d', finishedAt / 1000, 'unixepoch', 'localtime'))
        FROM sessions
        """,
    )
    fun observeDaysPractised(): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(firstTryCorrect), 0) AS correctCount,
               COALESCE(SUM(total), 0) AS totalCount
        FROM sessions
        """,
    )
    fun observeAnswerTotals(): Flow<AnswerTotalsRow>

    @Query(
        """
        SELECT topic,
               SUM(CASE WHEN correct = 1 THEN 1 ELSE 0 END) AS correctCount,
               COUNT(*) AS totalCount
        FROM attempts
        GROUP BY topic
        ORDER BY topic
        """,
    )
    fun observeTopicAccuracy(): Flow<List<TopicAccuracyRow>>

    @Query("SELECT * FROM sessions ORDER BY finishedAt DESC, id DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<SessionEntity>>

    @Query("DELETE FROM attempts")
    suspend fun clearAttempts()

    @Query("DELETE FROM sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM lesson_progress")
    suspend fun clearLessonProgress()

    @Transaction
    suspend fun clearAll() {
        clearAttempts()
        clearSessions()
        clearLessonProgress()
    }
}
