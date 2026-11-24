package au.edu.jcu.codequest

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.edu.jcu.codequest.ui.onboarding.OnboardingScreen
import au.edu.jcu.codequest.ui.onboarding.OnboardingTestTags
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** GUI tests for the consent screen, including the "equal-weight choices" design rule. */
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val choices = mutableListOf<Boolean>()

    /** Shows the screen and moves past the story step to the consent question. */
    private fun showScreen() {
        composeRule.setContent {
            CodeQuestTheme(darkTheme = false) {
                OnboardingScreen(onChoice = { choices += it })
            }
        }
        composeRule.onNodeWithTag(OnboardingTestTags.NEXT).performScrollTo().performClick()
    }

    @Test
    fun storyStep_comesFirstAndRecordsNoChoice() {
        composeRule.setContent {
            CodeQuestTheme(darkTheme = false) {
                OnboardingScreen(onChoice = { choices += it })
            }
        }

        composeRule.onNodeWithTag(OnboardingTestTags.NEXT).assertHasClickAction()
        composeRule.onNodeWithTag(OnboardingTestTags.ACCEPT).assertDoesNotExist()
        assertEquals(emptyList<Boolean>(), choices)
    }

    @Test
    fun decline_reportsThatHistoryShouldNotBeSaved() {
        showScreen()

        composeRule.onNodeWithTag(OnboardingTestTags.DECLINE).performScrollTo().performClick()

        assertEquals(listOf(false), choices)
    }

    @Test
    fun accept_reportsThatHistoryShouldBeSaved() {
        showScreen()

        composeRule.onNodeWithTag(OnboardingTestTags.ACCEPT).performScrollTo().performClick()

        assertEquals(listOf(true), choices)
    }

    @Test
    fun acceptAndDecline_areTheSameSize() {
        showScreen()

        val accept = composeRule.onNodeWithTag(OnboardingTestTags.ACCEPT).assertHasClickAction()
            .fetchSemanticsNode().size
        val decline = composeRule.onNodeWithTag(OnboardingTestTags.DECLINE).assertHasClickAction()
            .fetchSemanticsNode().size

        assertEquals("Neither consent answer may be visually favoured", accept, decline)
    }
}
