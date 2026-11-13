package au.edu.jcu.codequest.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import au.edu.jcu.codequest.domain.model.QuestionType

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val unitIndex: Int,
    val unitTitle: String,
    val orderIndex: Int,
    val title: String,
    val description: String,
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("lessonId")],
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val orderIndex: Int,
    val topic: String,
    val type: QuestionType,
    val prompt: String,
    val code: String?,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

/**
 * Best result per lesson. Deliberately has no foreign key to [LessonEntity] so that refreshing
 * the bundled course never deletes what the learner has achieved.
 */
@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val bestStars: Int,
    val timesCompleted: Int,
    val lastCompletedAt: Long,
)

/** One finished lesson or bonus round. Holds totals only, never individual answers. */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: String,
    val lessonTitle: String,
    val total: Int,
    val firstTryCorrect: Int,
    val stars: Int,
    val xp: Int,
    val finishedAt: Long,
)

/** One submitted answer. Rows exist only while the learner has opted in to detailed history. */
@Entity(tableName = "attempts", indices = [Index("topic")])
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val topic: String,
    val correct: Boolean,
    val answeredAt: Long,
)

/** Bonus-round question downloaded from Open Trivia DB, kept so the last round works offline. */
@Entity(tableName = "trivia_cache", indices = [Index("difficulty")])
data class TriviaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val difficulty: String,
    val question: String,
    val correctAnswer: String,
    val incorrectAnswers: List<String>,
    val fetchedAt: Long,
)

/** Query result: a lesson joined with the learner's progress (zeros when not started). */
data class LessonWithProgress(
    @Embedded val lesson: LessonEntity,
    val bestStars: Int,
    val timesCompleted: Int,
)

/** Query result: answers grouped by topic. */
data class TopicAccuracyRow(
    val topic: String,
    val correctCount: Int,
    val totalCount: Int,
)

/** Query result: first-try totals across all sessions. */
data class AnswerTotalsRow(
    val correctCount: Int,
    val totalCount: Int,
)
