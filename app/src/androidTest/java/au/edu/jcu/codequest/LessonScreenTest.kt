package au.edu.jcu.codequest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.edu.jcu.codequest.domain.model.Question
import au.edu.jcu.codequest.domain.model.QuestionType
import au.edu.jcu.codequest.ui.lesson.Feedback
import au.edu.jcu.codequest.ui.lesson.LessonScreen
import au.edu.jcu.codequest.ui.lesson.LessonTestTags
import au.edu.jcu.codequest.ui.lesson.LessonUiState
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** GUI tests for the activity screen. The screen is stateless, so each test supplies a state. */
@RunWith(AndroidJUnit4::class)
class LessonScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val question = Question(
        id = "q1",
        lessonId = "lesson",
        topic = "Variables",
        type = QuestionType.CHOICE,
        prompt = "Which keyword declares a read-only variable?",
        options = listOf("var", "val", "let"),
        correctIndex = 1,
        explanation = "val creates a read-only reference.",
    )

    private val baseState = LessonUiState(isLoading = false, title = "Values", question = question, total = 5)

    private val selectedOptions = mutableListOf<Int>()
    private var checkClicks = 0
    private var continueClicks = 0

    private fun showScreen(state: LessonUiState) {
        composeRule.setContent {
            CodeQuestTheme(darkTheme = false) {
                LessonScreen(
                    uiState = state,
                    onSelectChoice = { selectedOptions += it },
                    onPickLine = {},
                    onRemoveLine = {},
                    onCheck = { checkClicks++ },
                    onContinue = { continueClicks++ },
                    onRetryLoad = {},
                    onClose = {},
                )
            }
        }
    }

    @Test
    fun question_showsPromptAndOptions_withCheckDisabledUntilAnswered() {
        showScreen(baseState)

        composeRule.onNodeWithText(question.prompt).assertIsDisplayed()
        composeRule.onNodeWithText("val").assertIsDisplayed()
        composeRule.onNodeWithTag(LessonTestTags.CHECK).assertIsNotEnabled()
    }

    @Test
    fun tappingAnOption_reportsItsIndex() {
        showScreen(baseState)

        composeRule.onNodeWithTag(LessonTestTags.option(1)).performClick()

        assertEquals(listOf(1), selectedOptions)
    }

    @Test
    fun withASelection_checkIsEnabledAndClickable() {
        showScreen(baseState.copy(selectedChoice = 1))

        composeRule.onNodeWithTag(LessonTestTags.CHECK).assertIsEnabled().performClick()

        assertEquals(1, checkClicks)
    }

    @Test
    fun wrongAnswerFeedback_explainsAndOffersContinue() {
        val feedback = Feedback(correct = false, correctAnswer = "val", explanation = question.explanation)
        showScreen(baseState.copy(selectedChoice = 0, feedback = feedback))

        composeRule.onNodeWithTag(LessonTestTags.FEEDBACK).assertIsDisplayed()
        composeRule.onNodeWithText(question.explanation).assertIsDisplayed()
        composeRule.onNodeWithTag(LessonTestTags.CONTINUE).assertIsEnabled().performClick()

        assertEquals(1, continueClicks)
        assertEquals(0, checkClicks)
    }
}
