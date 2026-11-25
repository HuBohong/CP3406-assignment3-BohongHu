package au.edu.jcu.codequest.data.repository

import au.edu.jcu.codequest.domain.mission.BlockCollection
import au.edu.jcu.codequest.domain.mission.PaletteBlock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Which code blocks the learner has collected so far. */
fun interface BlockCollectionRepository {
    fun observeCollected(): Flow<Set<PaletteBlock>>
}

/**
 * Collected blocks follow completed lessons. With "Unlock all lessons" switched on (for
 * teachers and markers exploring the app) every block is available, matching the open path.
 */
class ProgressBlockCollectionRepository @Inject constructor(
    private val curriculumRepository: CurriculumRepository,
    private val settingsRepository: SettingsRepository,
) : BlockCollectionRepository {

    override fun observeCollected(): Flow<Set<PaletteBlock>> =
        combine(curriculumRepository.observeLessons(), settingsRepository.settings) { lessons, settings ->
            if (settings.unlockAllLessons) {
                BlockCollection.all.toSet()
            } else {
                BlockCollection.collected(lessons.filter { it.isCompleted }.map { it.lesson.id }.toSet())
            }
        }
}
