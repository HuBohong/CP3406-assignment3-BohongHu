package au.edu.jcu.codequest

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.edu.jcu.codequest.domain.flight.Flight
import au.edu.jcu.codequest.domain.flight.FlightCatalog
import au.edu.jcu.codequest.ui.flight.FlightScreen
import au.edu.jcu.codequest.ui.flight.FlightTestTags
import au.edu.jcu.codequest.ui.flight.FlightUiState
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** GUI tests for the rocket flight: the launch(angle, power) dials, Launch, and the result panel. */
@RunWith(AndroidJUnit4::class)
class FlightScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val level = FlightCatalog.toPlanetTwo
    private val events = mutableListOf<String>()

    private fun showScreen(state: FlightUiState) {
        composeRule.setContent {
            CodeQuestTheme(darkTheme = false) {
                FlightScreen(
                    uiState = state,
                    onExit = { events += "exit" },
                    onChangeAngle = { events += "angle$it" },
                    onChangePower = { events += "power$it" },
                    onLaunch = { events += "launch" },
                    onAdjust = { events += "adjust" },
                )
            }
        }
    }

    @Test
    fun dialsAndLaunch_reportWhatWasTapped() {
        showScreen(FlightUiState(level = level, angle = 30, power = 40))

        composeRule.onNodeWithTag(FlightTestTags.dial("angle", +1)).performClick()
        composeRule.onNodeWithTag(FlightTestTags.dial("power", -1)).performClick()
        composeRule.onNodeWithTag(FlightTestTags.LAUNCH).assertIsEnabled().performClick()

        assertEquals(listOf("angle1", "power-1", "launch"), events)
    }

    @Test
    fun aMissedFlight_explainsWhy_andOffersToAdjust() {
        val flight = Flight.simulate(level, angle = 30, power = 40)
        showScreen(FlightUiState(level = level, angle = 30, power = 40, flight = flight, shownPoints = flight.path.size, launches = 1))

        composeRule.onNodeWithTag(FlightTestTags.RESULT).assertExists()
        composeRule.onNodeWithText("Too short").assertExists()
        composeRule.onNodeWithTag(FlightTestTags.LAUNCH).assertIsNotEnabled()
        composeRule.onNodeWithText("Adjust and try again").performClick()

        assertEquals(listOf("adjust"), events)
    }

    @Test
    fun aLanding_celebrates_andLeadsBackToTheMap() {
        val flight = Flight.simulate(level, angle = 45, power = 50)
        showScreen(FlightUiState(level = level, angle = 45, power = 50, flight = flight, shownPoints = flight.path.size, launches = 1))

        composeRule.onNodeWithText("Touchdown!").assertExists()
        composeRule.onNodeWithText("Back to map").performClick()

        assertEquals(listOf("exit"), events)
    }
}
