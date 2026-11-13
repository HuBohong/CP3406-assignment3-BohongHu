package au.edu.jcu.codequest.domain

import au.edu.jcu.codequest.domain.model.Answer
import au.edu.jcu.codequest.domain.model.QuestionType
import au.edu.jcu.codequest.domain.model.isCorrect
import au.edu.jcu.codequest.domain.quiz.TriviaQuestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriviaQuestionsTest {

    @Test
    fun decode_handlesPercentEncodingAndKeepsPlusSigns() {
        assertEquals("What does \"CPU\" stand for?", TriviaQuestions.decode("What%20does%20%22CPU%22%20stand%20for%3F"))
        assertEquals("C++", TriviaQuestions.decode("C%2B%2B"))
        assertEquals("C++", TriviaQuestions.decode("C++"))
    }

    @Test
    fun decode_returnsInputWhenItIsNotValidEncoding() {
        assertEquals("100% sure", TriviaQuestions.decode("100% sure"))
    }

    @Test
    fun build_createsChoiceQuestionWithCorrectAnswerAmongOptions() {
        val question = TriviaQuestions.build(
            id = "bonus-1",
            question = "Which company created Kotlin?",
            correctAnswer = "JetBrains",
            incorrectAnswers = listOf("Google", "Oracle", "Microsoft"),
        )

        assertEquals(QuestionType.CHOICE, question.type)
        assertEquals(4, question.options.size)
        assertEquals("JetBrains", question.options[question.correctIndex])
        assertTrue(question.isCorrect(Answer.Choice(question.correctIndex)))
        assertEquals(TriviaQuestions.LESSON_ID, question.lessonId)
    }

    @Test
    fun build_isRepeatableForTheSameQuestion() {
        fun build() = TriviaQuestions.build("bonus-1", "Q?", "right", listOf("w1", "w2", "w3"))

        assertEquals(build().options, build().options)
    }
}
