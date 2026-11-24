package au.edu.jcu.codequest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LessonEntity::class,
        QuestionEntity::class,
        LessonProgressEntity::class,
        SessionEntity::class,
        AttemptEntity::class,
        TriviaEntity::class,
        MissionProgressEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun curriculumDao(): CurriculumDao
    abstract fun progressDao(): ProgressDao
    abstract fun triviaDao(): TriviaDao
    abstract fun missionDao(): MissionDao

    companion object {
        const val NAME = "codequest.db"

        /**
         * Version 2 adds the mission_progress table. Existing lessons, sessions and answers are
         * kept, so learners who installed version 1 keep all their progress after updating.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mission_progress` (
                        `missionId` TEXT NOT NULL,
                        `completed` INTEGER NOT NULL,
                        `attempts` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        PRIMARY KEY(`missionId`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
