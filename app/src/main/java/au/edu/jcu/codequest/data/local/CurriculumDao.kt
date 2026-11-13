package au.edu.jcu.codequest.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Read access to the course, plus the one-off copy of the bundled content into the database. */
@Dao
interface CurriculumDao {

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun lessonCount(): Int

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun questionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    /** Deleting lessons also deletes their questions through the foreign key cascade. */
    @Query("DELETE FROM lessons")
    suspend fun clearLessons()

    @Transaction
    suspend fun replaceCurriculum(lessons: List<LessonEntity>, questions: List<QuestionEntity>) {
        clearLessons()
        insertLessons(lessons)
        insertQuestions(questions)
    }

    /** Emits again whenever a lesson or the learner's progress changes. */
    @Query(
        """
        SELECT l.*,
               COALESCE(p.bestStars, 0) AS bestStars,
               COALESCE(p.timesCompleted, 0) AS timesCompleted
        FROM lessons AS l
        LEFT JOIN lesson_progress AS p ON p.lessonId = l.id
        ORDER BY l.unitIndex, l.orderIndex
        """,
    )
    fun observeLessonsWithProgress(): Flow<List<LessonWithProgress>>

    @Query("SELECT * FROM lessons WHERE id = :lessonId")
    suspend fun getLesson(lessonId: String): LessonEntity?

    @Query("SELECT * FROM questions WHERE lessonId = :lessonId ORDER BY orderIndex")
    suspend fun getQuestions(lessonId: String): List<QuestionEntity>
}
