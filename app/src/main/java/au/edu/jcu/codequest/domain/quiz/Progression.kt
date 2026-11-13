package au.edu.jcu.codequest.domain.quiz

import au.edu.jcu.codequest.domain.model.LessonSummary

/** Rules for moving along the learning path. Expects lessons in path order. */
object Progression {

    /**
     * Ids of lessons that cannot be opened yet. A lesson opens once the one before it is
     * completed. The learner can switch this gate off in Settings ([unlockAll]).
     */
    fun lockedLessonIds(lessons: List<LessonSummary>, unlockAll: Boolean): Set<String> {
        if (unlockAll) return emptySet()
        return lessons
            .filterIndexed { index, _ -> index > 0 && !lessons[index - 1].isCompleted }
            .map { it.lesson.id }
            .toSet()
    }

    /** The lesson "Continue" should open: the first unfinished one, or the first for revision. */
    fun nextLesson(lessons: List<LessonSummary>): LessonSummary? =
        lessons.firstOrNull { !it.isCompleted } ?: lessons.firstOrNull()
}
