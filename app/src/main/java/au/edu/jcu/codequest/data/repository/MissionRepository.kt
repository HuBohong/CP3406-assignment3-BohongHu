package au.edu.jcu.codequest.data.repository

import au.edu.jcu.codequest.data.local.MissionDao
import au.edu.jcu.codequest.util.TimeProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Which end-of-unit missions the learner has completed. Stored on the device only. */
interface MissionRepository {
    fun observeCompletedMissionIds(): Flow<Set<String>>
    suspend fun recordAttempt(missionId: String, success: Boolean)
}

class OfflineMissionRepository @Inject constructor(
    private val missionDao: MissionDao,
    private val timeProvider: TimeProvider,
) : MissionRepository {

    override fun observeCompletedMissionIds(): Flow<Set<String>> =
        missionDao.observeCompletedIds().map { it.toSet() }

    override suspend fun recordAttempt(missionId: String, success: Boolean) {
        missionDao.recordAttempt(missionId, success, timeProvider.now())
    }
}
