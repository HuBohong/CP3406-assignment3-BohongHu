package au.edu.jcu.codequest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        LessonEntity::class,
        QuestionEntity::class,
        LessonProgressEntity::class,
        SessionEntity::class,
        AttemptEntity::class,
        TriviaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun curriculumDao(): CurriculumDao
    abstract fun progressDao(): ProgressDao
    abstract fun triviaDao(): TriviaDao

    companion object {
        const val NAME = "codequest.db"
    }
}
