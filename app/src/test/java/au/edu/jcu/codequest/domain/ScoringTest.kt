package au.edu.jcu.codequest.domain

import au.edu.jcu.codequest.domain.quiz.Scoring
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringTest {

    @Test
    fun stars_perfectRunEarnsThree() {
        assertEquals(3, Scoring.stars(firstTryCorrect = 5, total = 5))
    }

    @Test
    fun stars_oneMistakeInFiveEarnsTwo() {
        assertEquals(2, Scoring.stars(firstTryCorrect = 4, total = 5))
        assertEquals(2, Scoring.stars(firstTryCorrect = 3, total = 5))
    }

    @Test
    fun stars_finishingAlwaysEarnsAtLeastOne() {
        assertEquals(1, Scoring.stars(firstTryCorrect = 2, total = 5))
        assertEquals(1, Scoring.stars(firstTryCorrect = 0, total = 5))
    }

    @Test
    fun stars_emptySessionEarnsNone() {
        assertEquals(0, Scoring.stars(firstTryCorrect = 0, total = 0))
    }

    @Test
    fun xp_rewardsCompletionPlusFirstTryBonus() {
        assertEquals(75, Scoring.xp(firstTryCorrect = 5, total = 5))
        assertEquals(50, Scoring.xp(firstTryCorrect = 0, total = 5))
    }

    @Test
    fun accuracyPercent_roundsAndHandlesZero() {
        assertEquals(67, Scoring.accuracyPercent(correct = 2, total = 3))
        assertEquals(100, Scoring.accuracyPercent(correct = 4, total = 4))
        assertEquals(0, Scoring.accuracyPercent(correct = 0, total = 0))
    }
}
