package au.edu.jcu.codequest.ui

import au.edu.jcu.codequest.domain.model.SessionResult
import au.edu.jcu.codequest.ui.navigation.Destination
import au.edu.jcu.codequest.ui.navigation.Routes
import kotlinx.serialization.descriptors.elementNames
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Type-safe navigation hands each route property to the screen's SavedStateHandle under its
 * name. These tests keep those names in step with the keys the ViewModels read.
 */
class DestinationTest {

    @Test
    fun routeArguments_matchTheKeysViewModelsRead() {
        assertEquals(listOf(Routes.ARG_LESSON_ID), Destination.Lesson.serializer().descriptor.elementNames.toList())
        assertEquals(listOf(Routes.ARG_MISSION_ID), Destination.Mission.serializer().descriptor.elementNames.toList())
        assertEquals(listOf(Routes.ARG_FLIGHT_ID), Destination.Flight.serializer().descriptor.elementNames.toList())
    }

    @Test
    fun resultRoute_carriesEverythingTheResultScreenShows() {
        val session = SessionResult(
            lessonId = "flow-2",
            lessonTitle = "Loops and ranges",
            total = 5,
            firstTryCorrect = 5,
            stars = 3,
            xp = 75,
            isBonus = false,
            firstCompletion = true,
        )

        assertEquals(
            Destination.Result(lessonId = "flow-2", stars = 3, xp = 75, firstTry = 5, total = 5, firstTime = true),
            Destination.Result.from(session),
        )
    }
}
