package au.edu.jcu.codequest.domain.quiz

import au.edu.jcu.codequest.domain.model.Question
import au.edu.jcu.codequest.domain.model.QuestionType
import java.net.URLDecoder
import kotlin.random.Random

/** Pure helpers that turn Open Trivia DB data into [Question]s. */
object TriviaQuestions {
    const val LESSON_ID = "bonus"
    const val TOPIC = "CS trivia"

    /** Name stored with a finished bonus round, shown in the recent-sessions list. */
    const val TITLE = "Bonus round"

    /**
     * Decodes text requested with `encode=url3986`. A literal "+" is protected first because
     * [URLDecoder] would otherwise read it as a space (RFC 3986 encodes spaces as %20).
     */
    fun decode(text: String): String = try {
        URLDecoder.decode(text.replace("+", "%2B"), "UTF-8")
    } catch (e: IllegalArgumentException) {
        text
    }

    /**
     * Builds a multiple-choice question. Options are shuffled with a seed taken from the question
     * text, so the correct answer is not always in the same place but the order is repeatable.
     */
    fun build(
        id: String,
        question: String,
        correctAnswer: String,
        incorrectAnswers: List<String>,
    ): Question {
        val options = (incorrectAnswers + correctAnswer).distinct().shuffled(Random(question.hashCode()))
        return Question(
            id = id,
            lessonId = LESSON_ID,
            topic = TOPIC,
            type = QuestionType.CHOICE,
            prompt = question,
            options = options,
            correctIndex = options.indexOf(correctAnswer),
            explanation = "The correct answer is: $correctAnswer",
        )
    }
}
