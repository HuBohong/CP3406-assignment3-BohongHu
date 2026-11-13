package au.edu.jcu.codequest.domain.model

/** Outcome of one finished lesson or bonus round. */
data class SessionResult(
    val lessonId: String,
    val lessonTitle: String,
    val total: Int,
    val firstTryCorrect: Int,
    val stars: Int,
    val xp: Int,
    val isBonus: Boolean,
)

/** One submitted answer. Only stored when the learner opted in to detailed history. */
data class AttemptRecord(
    val questionId: String,
    val topic: String,
    val correct: Boolean,
)

/** Headline numbers for the statistics screen. */
data class StatsOverview(
    val totalXp: Int = 0,
    val sessionsCompleted: Int = 0,
    val daysPractised: Int = 0,
    val firstTryCorrect: Int = 0,
    val questionsAnswered: Int = 0,
)

data class TopicAccuracy(
    val topic: String,
    val correct: Int,
    val total: Int,
)

data class RecentSession(
    val id: Long,
    val lessonTitle: String,
    val stars: Int,
    val xp: Int,
    val firstTryCorrect: Int,
    val total: Int,
    val finishedAt: Long,
)
