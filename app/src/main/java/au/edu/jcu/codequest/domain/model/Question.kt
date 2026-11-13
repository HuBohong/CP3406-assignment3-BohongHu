package au.edu.jcu.codequest.domain.model

import kotlin.random.Random

/** How a question is answered on screen. */
enum class QuestionType {
    /** Pick one of several options (also used for "predict the output" and "fill the blank"). */
    CHOICE,

    /** Put shuffled lines of code back into a working order (a Parsons problem). */
    ORDER,
}

/**
 * One exercise in a lesson.
 *
 * For [QuestionType.CHOICE], [options] are the answers and [correctIndex] points at the right one.
 * For [QuestionType.ORDER], [options] holds the code lines in their correct order and
 * [correctIndex] is unused.
 */
data class Question(
    val id: String,
    val lessonId: String,
    val topic: String,
    val type: QuestionType,
    val prompt: String,
    val code: String? = null,
    val options: List<String>,
    val correctIndex: Int = NO_INDEX,
    val explanation: String,
) {
    companion object {
        const val NO_INDEX = -1
    }
}

/** What the learner submitted for a question. */
sealed interface Answer {
    data class Choice(val index: Int) : Answer
    data class Order(val lines: List<String>) : Answer
}

/** True when [answer] solves this question. An answer of the wrong kind is never correct. */
fun Question.isCorrect(answer: Answer): Boolean = when (answer) {
    is Answer.Choice -> type == QuestionType.CHOICE && answer.index == correctIndex
    is Answer.Order -> type == QuestionType.ORDER && answer.lines == options
}

/** The model answer as text, shown in the feedback panel after a wrong attempt. */
fun Question.correctAnswerText(): String = when (type) {
    QuestionType.CHOICE -> options.getOrElse(correctIndex) { "" }
    QuestionType.ORDER -> options.joinToString(separator = "\n")
}

/**
 * The lines of an [QuestionType.ORDER] question in a scrambled order.
 *
 * The shuffle is seeded from the question id, so the same question always starts in the same
 * arrangement (stable across rotation and in tests) and never starts already solved.
 */
fun Question.scrambledLines(): List<String> {
    if (options.size < 2) return options
    val shuffled = options.shuffled(Random(id.hashCode()))
    return if (shuffled == options) options.drop(1) + options.first() else shuffled
}
