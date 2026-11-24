package au.edu.jcu.codequest.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {

    @Query("SELECT missionId FROM mission_progress WHERE completed = 1")
    fun observeCompletedIds(): Flow<List<String>>

    @Query("SELECT * FROM mission_progress WHERE missionId = :missionId")
    suspend fun get(missionId: String): MissionProgressEntity?

    @Upsert
    suspend fun upsert(progress: MissionProgressEntity)

    /**
     * Counts one run of a mission. Once completed, a mission stays completed and keeps the time
     * it was first completed, so replaying it for fun can never undo progress.
     */
    @Transaction
    suspend fun recordAttempt(missionId: String, success: Boolean, now: Long) {
        val existing = get(missionId)
        val wasCompleted = existing?.completed == true
        upsert(
            MissionProgressEntity(
                missionId = missionId,
                completed = wasCompleted || success,
                attempts = (existing?.attempts ?: 0) + 1,
                completedAt = existing?.completedAt ?: if (success) now else null,
            ),
        )
    }
}
