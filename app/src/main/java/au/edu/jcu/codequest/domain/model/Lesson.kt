package au.edu.jcu.codequest.domain.model

/** A lesson on the learning path. Lessons are grouped into units by [unitIndex]. */
data class Lesson(
    val id: String,
    val unitIndex: Int,
    val unitTitle: String,
    val orderIndex: Int,
    val title: String,
    val description: String,
)

/** A lesson together with the learner's best result for it. */
data class LessonSummary(
    val lesson: Lesson,
    val bestStars: Int = 0,
    val timesCompleted: Int = 0,
) {
    val isCompleted: Boolean get() = timesCompleted > 0
}
