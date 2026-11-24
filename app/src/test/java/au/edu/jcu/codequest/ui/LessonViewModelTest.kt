package au.edu.jcu.codequest.ui

import androidx.lifecycle.SavedStateHandle
import au.edu.jcu.codequest.data.repository.LearnCardRepository
import au.edu.jcu.codequest.domain.learn.LearnCard
import au.edu.jcu.codequest.domain.learn.LearnCatalog
import au.edu.jcu.codequest.data.repository.BonusRound
import au.edu.jcu.codequest.domain.model.Difficulty
import au.edu.jcu.codequest.domain.model.UserSettings
import au.edu.jcu.codequest.testutil.FakeCurriculumRepository
import au.edu.jcu.codequest.testutil.FakeProgressRepository
import au.edu.jcu.codequest.testutil.FakeSettingsRepository
import au.edu.jcu.codequest.testutil.FakeSoundPlayer
import au.edu.jcu.codequest.testutil.FakeTriviaRepository
import au.edu.jcu.codequest.testutil.MainDispatcherRule
import au.edu.jcu.codequest.testutil.testChoiceQuestion
import au.edu.jcu.codequest.testutil.testOrderQuestion
import au.edu.jcu.codequest.testutil.testSummary
import au.edu.jcu.codequest.ui.lesson.LessonViewModel
import au.edu.jcu.codequest.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The Main dispatcher is unconfined, so every launched coroutine finishes before the next line. */
class LessonViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val q1 = testChoiceQuestion("q1")
    private val q2 = testChoiceQuestion("q2")
    private val orderQuestion = testOrderQuestion("order")

    private val curriculum = FakeCurriculumRepository(
        lessons = listOf(testSummary("lesson"), testSummary("ordering")),
        questions = mapOf("lesson" to listOf(q1, q2), "ordering" to listOf(orderQuestion)),
    )
    private val trivia = FakeTriviaRepository()
    private val progress = FakeProgressRepository()
    private val settings = FakeSettingsRepository(UserSettings(onboardingDone = true))
    private val sound = FakeSoundPlayer()
    private var cards: Map<String, List<LearnCard>> = emptyMap()
    private val learnCards = LearnCardRepository { lessonId -> cards[lessonId].orEmpty() }

    private fun createViewModel(lessonId: String = "lesson") = LessonViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_LESSON_ID to lessonId)),
        curriculumRepository = curriculum,
        triviaRepository = trivia,
        progressRepository = progress,
        settingsRepository = settings,
        soundPlayer = sound,
        learnCardRepository = learnCards,
    )

    private fun LessonViewModel.answerCurrent(correctly: Boolean) {
        selectChoice(if (correctly) 1 else 0)
        check()
        next()
    }

    @Test
    fun load_showsFirstQuestionOfTheLesson() {
        val state = createViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("q1", state.question?.id)
        assertEquals("Lesson lesson", state.title)
        assertEquals(2, state.total)
        assertFalse(state.canCheck)
    }

    @Test
    fun check_withoutAnAnswer_doesNothing() {
        val viewModel = createViewModel()

        viewModel.check()

        assertNull(viewModel.uiState.value.feedback)
        assertTrue(sound.played.isEmpty())
    }

    @Test
    fun check_correctAnswer_showsFeedbackAndKeepsQuestionUntilNext() {
        val viewModel = createViewModel()

        viewModel.selectChoice(1)
        viewModel.check()

        val state = viewModel.uiState.value
        assertEquals(true, state.feedback?.correct)
        assertEquals("Explanation for q1", state.feedback?.explanation)
        assertEquals("q1", state.question?.id)

        viewModel.next()

        assertEquals("q2", viewModel.uiState.value.question?.id)
        assertEquals(1, viewModel.uiState.value.mastered)
        assertNull(viewModel.uiState.value.feedback)
    }

    @Test
    fun selection_isLockedOnceFeedbackIsShown() {
        val viewModel = createViewModel()

        viewModel.selectChoice(0)
        viewModel.check()
        viewModel.selectChoice(1)

        assertEquals(0, viewModel.uiState.value.selectedChoice)
    }

    @Test
    fun wrongAnswer_bringsTheQuestionBackAtTheEnd() {
        val viewModel = createViewModel()

        viewModel.selectChoice(0)
        viewModel.check()
        assertEquals(false, viewModel.uiState.value.feedback?.correct)
        assertEquals("right", viewModel.uiState.value.feedback?.correctAnswer)

        viewModel.next()
        assertEquals("q2", viewModel.uiState.value.question?.id)

        viewModel.answerCurrent(correctly = true)

        val state = viewModel.uiState.value
        assertEquals("q1", state.question?.id)
        assertTrue(state.isReviewingMistake)
        assertNull(state.result)
    }

    @Test
    fun finishingWithoutMistakes_recordsThreeStarsAndFullXp() {
        val viewModel = createViewModel()

        viewModel.answerCurrent(correctly = true)
        viewModel.answerCurrent(correctly = true)

        val result = viewModel.uiState.value.result
        assertNotNull(result)
        assertEquals(3, result?.stars)
        assertEquals(30, result?.xp)
        assertEquals(2, result?.firstTryCorrect)
        assertEquals(false, result?.isBonus)
        assertEquals(listOf(result), progress.recorded.map { it.result })
    }

    @Test
    fun finishingAfterAMistake_lowersStarsButStillCompletes() {
        val viewModel = createViewModel()

        viewModel.answerCurrent(correctly = false) // q1 wrong
        viewModel.answerCurrent(correctly = true) // q2
        viewModel.answerCurrent(correctly = true) // q1 again

        val result = viewModel.uiState.value.result
        assertEquals(1, result?.firstTryCorrect)
        assertEquals(1, result?.stars)
        assertEquals(25, result?.xp)
    }

    @Test
    fun detailedHistoryOff_sessionIsSavedWithoutAnswerHistory() {
        val viewModel = createViewModel()

        viewModel.answerCurrent(correctly = true)
        viewModel.answerCurrent(correctly = true)

        assertFalse(progress.recorded.single().saveAttempts)
    }

    @Test
    fun detailedHistoryOn_everyAttemptIsPassedOnForSaving() {
        settings.state.value = UserSettings(onboardingDone = true, detailedHistory = true)
        val viewModel = createViewModel()

        viewModel.answerCurrent(correctly = false)
        viewModel.answerCurrent(correctly = true)
        viewModel.answerCurrent(correctly = true)

        val recorded = progress.recorded.single()
        assertTrue(recorded.saveAttempts)
        assertEquals(listOf("q1", "q2", "q1"), recorded.attempts.map { it.questionId })
        assertEquals(listOf(false, true, true), recorded.attempts.map { it.correct })
    }

    @Test
    fun orderQuestion_canOnlyBeCheckedWhenEveryLineIsPlaced() {
        val viewModel = createViewModel("ordering")
        val pool = viewModel.uiState.value.orderPool
        assertEquals(orderQuestion.options.sorted(), pool.sorted())

        viewModel.pickLine(pool.indexOf("line one"))
        viewModel.pickLine(pool.indexOf("line two"))
        assertFalse(viewModel.uiState.value.canCheck)

        viewModel.pickLine(pool.indexOf("line three"))
        assertTrue(viewModel.uiState.value.canCheck)

        viewModel.check()
        assertEquals(true, viewModel.uiState.value.feedback?.correct)
    }

    @Test
    fun orderQuestion_wrongOrderIsMarkedIncorrect() {
        val viewModel = createViewModel("ordering")
        val pool = viewModel.uiState.value.orderPool

        listOf("line three", "line two", "line one").forEach { viewModel.pickLine(pool.indexOf(it)) }
        viewModel.check()

        assertEquals(false, viewModel.uiState.value.feedback?.correct)
    }

    @Test
    fun orderQuestion_linesCanBeRemovedAndAreNotPickedTwice() {
        val viewModel = createViewModel("ordering")

        viewModel.pickLine(0)
        viewModel.pickLine(0)
        viewModel.pickLine(2)
        assertEquals(listOf(0, 2), viewModel.uiState.value.pickedOrder)

        viewModel.removeLine(0)
        assertEquals(listOf(2), viewModel.uiState.value.pickedOrder)
    }

    @Test
    fun sound_playsOnlyWhenEnabled() {
        val viewModel = createViewModel()
        viewModel.selectChoice(1)
        viewModel.check()
        assertEquals(listOf(true), sound.played)

        settings.state.value = UserSettings(onboardingDone = true, soundOn = false)
        viewModel.next()
        viewModel.selectChoice(0)
        viewModel.check()
        assertEquals(listOf(true), sound.played)
    }

    @Test
    fun unknownLesson_reportsLoadFailure() {
        val state = createViewModel("missing").uiState.value

        assertTrue(state.loadFailed)
        assertNull(state.question)
    }

    @Test
    fun teachingCardsComeFirst_andEachMustBeReadyBeforeMovingOn() {
        cards = mapOf("lesson" to listOf(LearnCatalog.watchFuel, LearnCatalog.valBug))
        val viewModel = createViewModel()

        assertEquals(LearnCatalog.watchFuel, viewModel.uiState.value.currentCard)
        assertEquals(4, viewModel.uiState.value.stepsTotal)

        viewModel.nextCard()
        assertEquals("not ready yet", 0, viewModel.uiState.value.cardIndex)

        viewModel.markCardReady()
        viewModel.nextCard()
        assertEquals(LearnCatalog.valBug, viewModel.uiState.value.currentCard)
        assertFalse(viewModel.uiState.value.cardReady)
        assertEquals(0.25f, viewModel.uiState.value.progress, 0.001f)

        viewModel.markCardReady()
        viewModel.nextCard()
        assertNull(viewModel.uiState.value.currentCard)
        assertEquals("q1", viewModel.uiState.value.question?.id)
    }

    @Test
    fun questionsCannotBeCheckedWhileACardIsShowing() {
        cards = mapOf("lesson" to listOf(LearnCatalog.watchFuel))
        val viewModel = createViewModel()

        viewModel.selectChoice(1)
        viewModel.check()

        assertNull(viewModel.uiState.value.feedback)
    }

    @Test
    fun bonusRound_neverShowsTeachingCards() {
        cards = mapOf(Routes.BONUS_ID to listOf(LearnCatalog.watchFuel))
        trivia.round = BonusRound.Ready(listOf(testChoiceQuestion("bonus-1")), fromCache = false)

        val state = createViewModel(Routes.BONUS_ID).uiState.value

        assertNull(state.currentCard)
        assertEquals("bonus-1", state.question?.id)
    }

    @Test
    fun bonusRound_unavailable_thenRetrySucceeds() {
        val viewModel = createViewModel(Routes.BONUS_ID)
        assertTrue(viewModel.uiState.value.loadFailed)
        assertTrue(viewModel.uiState.value.isBonus)

        trivia.round = BonusRound.Ready(listOf(testChoiceQuestion("bonus-1")), fromCache = true)
        viewModel.load()

        val state = viewModel.uiState.value
        assertFalse(state.loadFailed)
        assertTrue(state.fromCache)
        assertEquals("bonus-1", state.question?.id)
    }

    @Test
    fun bonusRound_usesDifficultyFromSettingsAndIsRecordedAsBonus() {
        settings.state.value = UserSettings(onboardingDone = true, difficulty = Difficulty.HARD)
        trivia.round = BonusRound.Ready(listOf(testChoiceQuestion("bonus-1")), fromCache = false)
        val viewModel = createViewModel(Routes.BONUS_ID)

        viewModel.answerCurrent(correctly = true)

        assertEquals(listOf(Difficulty.HARD), trivia.requestedDifficulties)
        assertEquals(true, progress.recorded.single().result.isBonus)
    }
}
