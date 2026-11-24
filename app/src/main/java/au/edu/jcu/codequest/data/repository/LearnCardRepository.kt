package au.edu.jcu.codequest.data.repository

import au.edu.jcu.codequest.domain.learn.LearnCard
import au.edu.jcu.codequest.domain.learn.LearnCatalog
import javax.inject.Inject

/**
 * Teaching cards for a lesson. The cards are part of the app (each carries its own simulation
 * code), so this repository reads the built-in catalogue rather than the database.
 */
fun interface LearnCardRepository {
    fun cardsFor(lessonId: String): List<LearnCard>
}

class BuiltInLearnCardRepository @Inject constructor() : LearnCardRepository {
    override fun cardsFor(lessonId: String): List<LearnCard> = LearnCatalog.forLesson(lessonId)
}
