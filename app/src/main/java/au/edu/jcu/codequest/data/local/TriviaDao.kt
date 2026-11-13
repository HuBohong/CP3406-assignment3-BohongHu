package au.edu.jcu.codequest.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/** Offline cache of the most recent bonus round for each difficulty. */
@Dao
interface TriviaDao {

    @Query("SELECT * FROM trivia_cache WHERE difficulty = :difficulty ORDER BY id")
    suspend fun getByDifficulty(difficulty: String): List<TriviaEntity>

    @Insert
    suspend fun insertAll(questions: List<TriviaEntity>)

    @Query("DELETE FROM trivia_cache WHERE difficulty = :difficulty")
    suspend fun clearDifficulty(difficulty: String)

    @Transaction
    suspend fun replaceForDifficulty(difficulty: String, questions: List<TriviaEntity>) {
        clearDifficulty(difficulty)
        insertAll(questions)
    }
}
