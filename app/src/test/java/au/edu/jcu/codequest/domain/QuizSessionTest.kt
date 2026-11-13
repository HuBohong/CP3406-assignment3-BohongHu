package au.edu.jcu.codequest.domain

import au.edu.jcu.codequest.domain.model.Answer
import au.edu.jcu.codequest.domain.model.Question
import au.edu.jcu.codequest.domain.model.QuestionType
import au.edu.jcu.codequest.domain.model.correctAnswerText
import au.edu.jcu.codequest.domain.model.isCorrect
import au.edu.jcu.codequest.domain.model.scrambledLines
import au.edu.jcu.codequest.domain.quiz.QuizSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizSessionTest {

    private fun choiceQuestion(id: String) = Question(
        id = id,
        lessonId = "lesson",
        topic = "Topic",
        type = QuestionType.CHOICE,
        prompt = "Pick B",
        options = listOf("A", "B", "C"),
        correctIndex = 1,
        explanation = "Because.",
    )

    private val orderQuestion = Question(
        id = "order",
        lessonId = "lesson",
        topic = "Topic",
        type = QuestionType.ORDER,
        prompt = "Sort",
        options = listOf("first", "second", "third", "fourth"),
        explanation = "In order.",
    )

    @Test
    fun newSession_startsOnFirstQuestionWithNoProgress() {
        val session = QuizSession.start(listOf(choiceQuestion("q1"), choiceQuestion("q2")))

        assertEquals("q1", session.current?.id)
        assertEquals(2, session.total)
        assertEquals(0f, session.progress, 0f)
        assertFalse(session.isFinished)
    }

    @Test
    fun correctAnswer_movesOnAndCountsAsFirstTry() {
        val session = QuizSession.start(listOf(choiceQuestion("q1"), choiceQuestion("q2")))
            .answer(correct = true)

        assertEquals("q2", session.current?.id)
        assertEquals(1, session.mastered)
        assertEquals(1, session.firstTryCorrect)
        assertEquals(0.5f, session.progress, 0.001f)
    }

    @Test
    fun wrongAnswer_sendsQuestionToBackOfQueue() {
        val session = QuizSession.start(listOf(choiceQuestion("q1"), choiceQuestion("q2")))
            .answer(correct = false)

        assertEquals(listOf("q2", "q1"), session.pending.map { it.id })
        assertEquals(0, session.mastered)
        assertFalse(session.isReviewingMistake)
    }

    @Test
    fun retriedQuestion_isMasteredButNotCountedAsFirstTry() {
        val session = QuizSession.start(listOf(choiceQuestion("q1"), choiceQuestion("q2")))
            .answer(correct = false) // q1 missed
            .answer(correct = true) // q2 right first time

        assertTrue(session.isReviewingMistake)

        val finished = session.answer(correct = true) // q1 right on retry

        assertTrue(finished.isFinished)
        assertNull(finished.current)
        assertEquals(2, finished.mastered)
        assertEquals(1, finished.firstTryCorrect)
        assertEquals(1f, finished.progress, 0f)
    }

    @Test
    fun answeringAFinishedSession_changesNothing() {
        val finished = QuizSession.start(listOf(choiceQuestion("q1"))).answer(correct = true)

        assertEquals(finished, finished.answer(correct = true))
    }

    @Test
    fun emptySession_isFinishedImmediately() {
        val session = QuizSession.start(emptyList())

        assertTrue(session.isFinished)
        assertEquals(0f, session.progress, 0f)
    }

    @Test
    fun choiceQuestion_isCorrectOnlyForTheRightIndex() {
        val question = choiceQuestion("q1")

        assertTrue(question.isCorrect(Answer.Choice(1)))
        assertFalse(question.isCorrect(Answer.Choice(0)))
        assertFalse(question.isCorrect(Answer.Order(listOf("A", "B", "C"))))
        assertEquals("B", question.correctAnswerText())
    }

    @Test
    fun orderQuestion_isCorrectOnlyForTheExactOrder() {
        assertTrue(orderQuestion.isCorrect(Answer.Order(listOf("first", "second", "third", "fourth"))))
        assertFalse(orderQuestion.isCorrect(Answer.Order(listOf("second", "first", "third", "fourth"))))
        assertFalse(orderQuestion.isCorrect(Answer.Order(listOf("first", "second"))))
        assertFalse(orderQuestion.isCorrect(Answer.Choice(0)))
    }

    @Test
    fun scrambledLines_keepTheSameLinesButNeverStartSolved() {
        val scrambled = orderQuestion.scrambledLines()

        assertEquals(orderQuestion.options.sorted(), scrambled.sorted())
        assertNotEquals(orderQuestion.options, scrambled)
        assertEquals("The shuffle must be repeatable", scrambled, orderQuestion.scrambledLines())
    }
}
