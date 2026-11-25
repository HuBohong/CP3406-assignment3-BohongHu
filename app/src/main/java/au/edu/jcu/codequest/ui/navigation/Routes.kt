package au.edu.jcu.codequest.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.model.SessionResult
import au.edu.jcu.codequest.domain.quiz.TriviaQuestions
import au.edu.jcu.codequest.ui.components.CodeQuestIcons

/** Every navigation route in the app, with helpers that build routes containing arguments. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    const val ARG_LESSON_ID = "lessonId"
    const val ARG_STARS = "stars"
    const val ARG_XP = "xp"
    const val ARG_FIRST_TRY = "firstTry"
    const val ARG_TOTAL = "total"
    const val ARG_MISSION_ID = "missionId"
    const val ARG_FIRST_TIME = "firstTime"
    const val ARG_FLIGHT_ID = "flightId"

    const val LESSON = "lesson/{$ARG_LESSON_ID}"
    const val MISSION = "mission/{$ARG_MISSION_ID}"
    const val FLIGHT = "flight/{$ARG_FLIGHT_ID}"
    const val RESULT = "result/{$ARG_LESSON_ID}/{$ARG_STARS}/{$ARG_XP}/{$ARG_FIRST_TRY}/{$ARG_TOTAL}/{$ARG_FIRST_TIME}"

    /** Lets other apps or adb open a lesson directly, e.g. codequest://lesson/basics-1 */
    const val LESSON_DEEP_LINK = "codequest://lesson/{$ARG_LESSON_ID}"

    /** The bonus round reuses the lesson screen under this reserved id. */
    const val BONUS_ID = TriviaQuestions.LESSON_ID

    fun lesson(lessonId: String) = "lesson/$lessonId"

    fun mission(missionId: String) = "mission/$missionId"

    fun flight(flightId: String) = "flight/$flightId"

    fun result(result: SessionResult) =
        "result/${result.lessonId}/${result.stars}/${result.xp}/${result.firstTryCorrect}/${result.total}/${result.firstCompletion}"
}

/** The three tabs shown in the bottom bar (phones) or navigation rail (wide screens). */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_learn, CodeQuestIcons.Path),
    STATS(Routes.STATS, R.string.nav_progress, CodeQuestIcons.Chart),
    SETTINGS(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
}
