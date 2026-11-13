package au.edu.jcu.codequest.domain.quiz

import kotlin.math.roundToInt

/** Rules that turn a finished session into stars and experience points. */
object Scoring {
    const val MAX_STARS = 3
    const val XP_PER_QUESTION = 10
    const val XP_FIRST_TRY_BONUS = 5

    private const val THREE_STAR_RATIO = 0.9
    private const val TWO_STAR_RATIO = 0.6

    /** Finishing always earns one star; first-try accuracy earns the rest. */
    fun stars(firstTryCorrect: Int, total: Int): Int {
        if (total <= 0) return 0
        val ratio = firstTryCorrect.toDouble() / total
        return when {
            ratio >= THREE_STAR_RATIO -> MAX_STARS
            ratio >= TWO_STAR_RATIO -> 2
            else -> 1
        }
    }

    fun xp(firstTryCorrect: Int, total: Int): Int =
        total * XP_PER_QUESTION + firstTryCorrect * XP_FIRST_TRY_BONUS

    /** Whole-number percentage, or 0 when nothing has been answered yet. */
    fun accuracyPercent(correct: Int, total: Int): Int =
        if (total <= 0) 0 else (correct * 100.0 / total).roundToInt()
}
