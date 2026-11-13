package au.edu.jcu.codequest.domain.quiz

import au.edu.jcu.codequest.domain.model.Question

/**
 * Immutable state of a lesson in progress.
 *
 * A question that is answered wrongly is not lost: it moves to the back of the queue and comes
 * round again, so a lesson always ends with every question answered correctly once. There are no
 * lives and no timer; mistakes only affect the star rating through [firstTryCorrect].
 */
data class QuizSession(
    val pending: List<Question>,
    val total: Int,
    val mastered: Int = 0,
    val firstTryCorrect: Int = 0,
    val missedIds: Set<String> = emptySet(),
) {
    /** The question to show now, or null when the lesson is finished. */
    val current: Question? get() = pending.firstOrNull()

    val isFinished: Boolean get() = pending.isEmpty()

    /** True when the current question was answered wrongly earlier in this session. */
    val isReviewingMistake: Boolean get() = current?.id in missedIds

    /** Fraction of questions mastered so far, from 0 to 1. */
    val progress: Float get() = if (total == 0) 0f else mastered.toFloat() / total

    /** Returns the session after the current question was answered. */
    fun answer(correct: Boolean): QuizSession {
        val question = current ?: return this
        val rest = pending.drop(1)
        return if (correct) {
            copy(
                pending = rest,
                mastered = mastered + 1,
                firstTryCorrect = firstTryCorrect + if (question.id in missedIds) 0 else 1,
            )
        } else {
            copy(pending = rest + question, missedIds = missedIds + question.id)
        }
    }

    companion object {
        fun start(questions: List<Question>): QuizSession =
            QuizSession(pending = questions, total = questions.size)
    }
}
