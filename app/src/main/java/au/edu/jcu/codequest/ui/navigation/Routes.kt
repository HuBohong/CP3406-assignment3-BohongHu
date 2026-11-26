package au.edu.jcu.codequest.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.model.SessionResult
import au.edu.jcu.codequest.domain.quiz.TriviaQuestions
import au.edu.jcu.codequest.ui.components.CodeQuestIcons
import kotlinx.serialization.Serializable

/**
 * Every screen in the app as a type-safe navigation destination (Navigation 2.8). Arguments are
 * ordinary constructor properties, so a wrong or missing argument is a compile error rather than
 * a crash, and Navigation passes each property to the screen's SavedStateHandle under its name.
 */
@Serializable
sealed interface Destination {
    @Serializable
    data object Onboarding : Destination

    @Serializable
    data object Home : Destination

    @Serializable
    data object Stats : Destination

    @Serializable
    data object Settings : Destination

    /** A lesson, or the bonus round under [Routes.BONUS_ID]. */
    @Serializable
    data class Lesson(val lessonId: String) : Destination

    /** A planet mission, a boss fight, or the free-build planet. */
    @Serializable
    data class Mission(val missionId: String) : Destination

    /** A rocket flight between two planets. */
    @Serializable
    data class Flight(val flightId: String) : Destination

    @Serializable
    data class Result(
        val lessonId: String,
        val stars: Int,
        val xp: Int,
        val firstTry: Int,
        val total: Int,
        /** True the first time the lesson is finished, when its code blocks are handed over. */
        val firstTime: Boolean,
    ) : Destination {
        companion object {
            fun from(result: SessionResult) = Result(
                lessonId = result.lessonId,
                stars = result.stars,
                xp = result.xp,
                firstTry = result.firstTryCorrect,
                total = result.total,
                firstTime = result.firstCompletion,
            )
        }
    }
}

/**
 * Names under which ViewModels read their arguments from SavedStateHandle. They must match the
 * property names in [Destination]; DestinationTest checks that they do.
 */
object Routes {
    const val ARG_LESSON_ID = "lessonId"
    const val ARG_MISSION_ID = "missionId"
    const val ARG_FLIGHT_ID = "flightId"

    /** Lets other apps or adb open a lesson directly, e.g. codequest://lesson/basics-1 */
    const val LESSON_DEEP_LINK_BASE = "codequest://lesson"

    /** The bonus round reuses the lesson screen under this reserved id. */
    const val BONUS_ID = TriviaQuestions.LESSON_ID
}

/** The three tabs shown in the bottom bar (phones) or navigation rail (wide screens). */
enum class TopLevelDestination(
    val destination: Destination,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(Destination.Home, R.string.nav_learn, CodeQuestIcons.Path),
    STATS(Destination.Stats, R.string.nav_progress, CodeQuestIcons.Chart),
    SETTINGS(Destination.Settings, R.string.nav_settings, Icons.Filled.Settings),
}
