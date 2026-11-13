package au.edu.jcu.codequest.domain.model

/** Difficulty of the online bonus round. [apiValue] is the value Open Trivia DB expects. */
enum class Difficulty(val apiValue: String) {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard"),
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Everything the learner can configure.
 *
 * Defaults are the privacy-protective choice: no detailed answer history is kept until the
 * learner opts in on the first-run screen or in Settings.
 */
data class UserSettings(
    val onboardingDone: Boolean = false,
    val detailedHistory: Boolean = false,
    val soundOn: Boolean = true,
    val difficulty: Difficulty = Difficulty.EASY,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val unlockAllLessons: Boolean = false,
)
