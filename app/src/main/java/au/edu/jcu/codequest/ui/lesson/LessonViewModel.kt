package au.edu.jcu.codequest.ui.lesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.codequest.data.repository.BonusRound
import au.edu.jcu.codequest.data.repository.CurriculumRepository
import au.edu.jcu.codequest.data.repository.LearnCardRepository
import au.edu.jcu.codequest.data.repository.ProgressRepository
import au.edu.jcu.codequest.data.repository.SettingsRepository
import au.edu.jcu.codequest.data.repository.TriviaRepository
import au.edu.jcu.codequest.domain.learn.LearnCard
import au.edu.jcu.codequest.domain.model.Answer
import au.edu.jcu.codequest.domain.model.AttemptRecord
import au.edu.jcu.codequest.domain.model.Question
import au.edu.jcu.codequest.domain.model.QuestionType
import au.edu.jcu.codequest.domain.model.SessionResult
import au.edu.jcu.codequest.domain.model.correctAnswerText
import au.edu.jcu.codequest.domain.model.isCorrect
import au.edu.jcu.codequest.domain.model.scrambledLines
import au.edu.jcu.codequest.domain.quiz.QuizSession
import au.edu.jcu.codequest.domain.quiz.Scoring
import au.edu.jcu.codequest.domain.quiz.TriviaQuestions
import au.edu.jcu.codequest.ui.navigation.Routes
import au.edu.jcu.codequest.util.SoundPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shown after "Check": whether the answer was right, the model answer and why. */
data class Feedback(
    val correct: Boolean,
    val correctAnswer: String,
    val explanation: String,
)

data class LessonUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isBonus: Boolean = false,
    /** Bonus round only: true when the questions come from the offline cache. */
    val fromCache: Boolean = false,
    val title: String = "",
    val question: Question? = null,
    val isReviewingMistake: Boolean = false,
    val selectedChoice: Int? = null,
    /** ORDER questions: the scrambled lines the learner picks from. */
    val orderPool: List<String> = emptyList(),
    /** ORDER questions: indexes into [orderPool], in the order the learner tapped them. */
    val pickedOrder: List<Int> = emptyList(),
    val feedback: Feedback? = null,
    val mastered: Int = 0,
    val total: Int = 0,
    /** Set once the lesson is finished and saved; the screen then navigates to the result. */
    val result: SessionResult? = null,
    /** Teaching cards shown before the first question. */
    val cards: List<LearnCard> = emptyList(),
    val cardIndex: Int = 0,
    /** True once the learner has interacted with the current card enough to move on. */
    val cardReady: Boolean = false,
    /** Cards of a lesson finished before can be skipped straight away when practising again. */
    val cardsSkippable: Boolean = false,
) {
    /** The teaching card on screen, or null once the questions have started. */
    val currentCard: LearnCard? get() = cards.getOrNull(cardIndex)

    /** Cards and questions both count towards the progress bar. */
    val stepsDone: Int get() = minOf(cardIndex, cards.size) + mastered
    val stepsTotal: Int get() = cards.size + total
    val progress: Float get() = if (stepsTotal == 0) 0f else stepsDone.toFloat() / stepsTotal

    /** True when enough has been entered for "Check" to make sense. */
    val canCheck: Boolean
        get() = when (question?.type) {
            QuestionType.CHOICE -> selectedChoice != null
            QuestionType.ORDER -> orderPool.isNotEmpty() && pickedOrder.size == orderPool.size
            null -> false
        }
}

/**
 * Runs one lesson or bonus round. The quiz rules live in [QuizSession]; this class connects
 * them to the repositories and to what the screen shows.
 */
@HiltViewModel
class LessonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val curriculumRepository: CurriculumRepository,
    private val triviaRepository: TriviaRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val soundPlayer: SoundPlayer,
    private val learnCardRepository: LearnCardRepository,
) : ViewModel() {

    private val lessonId: String = checkNotNull(savedStateHandle.get<String>(Routes.ARG_LESSON_ID)) {
        "LessonViewModel needs a lessonId navigation argument"
    }
    private val isBonus = lessonId == Routes.BONUS_ID

    private var session: QuizSession? = null
    private var lessonTitle = ""
    private val attempts = mutableListOf<AttemptRecord>()

    private val _uiState = MutableStateFlow(LessonUiState(isBonus = isBonus))
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Loads the questions. Also used by the "Try again" button after a failed download. */
    fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val questions = if (isBonus) loadBonusQuestions() else loadLessonQuestions()
            val cards = if (isBonus) emptyList() else learnCardRepository.cardsFor(lessonId)
            val skippable = !isBonus && cards.isNotEmpty() && isCompletedBefore()
            _uiState.update { it.copy(cards = cards, cardIndex = 0, cardReady = skippable, cardsSkippable = skippable) }
            if (questions.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            } else {
                attempts.clear()
                session = QuizSession.start(questions)
                showCurrentQuestion()
            }
        }
    }

    /** The card on screen reports that the learner has done what it asks. */
    fun markCardReady() {
        _uiState.update { if (it.currentCard != null) it.copy(cardReady = true) else it }
    }

    /** Moves past the current teaching card once it is ready. */
    fun nextCard() {
        _uiState.update { state ->
            if (state.currentCard != null && state.cardReady) {
                state.copy(cardIndex = state.cardIndex + 1, cardReady = state.cardsSkippable)
            } else {
                state
            }
        }
    }

    fun selectChoice(index: Int) {
        _uiState.update { state ->
            if (state.feedback != null) state else state.copy(selectedChoice = index)
        }
    }

    /** ORDER questions: move a line from the pool to the end of the learner's program. */
    fun pickLine(poolIndex: Int) {
        _uiState.update { state ->
            val allowed = state.feedback == null &&
                poolIndex in state.orderPool.indices &&
                poolIndex !in state.pickedOrder
            if (allowed) state.copy(pickedOrder = state.pickedOrder + poolIndex) else state
        }
    }

    /** ORDER questions: put the line at [position] in the learner's program back in the pool. */
    fun removeLine(position: Int) {
        _uiState.update { state ->
            val allowed = state.feedback == null && position in state.pickedOrder.indices
            if (allowed) {
                state.copy(pickedOrder = state.pickedOrder.filterIndexed { index, _ -> index != position })
            } else {
                state
            }
        }
    }

    /** Marks the current answer and shows feedback. The question only advances on [next]. */
    fun check() {
        val state = _uiState.value
        val question = state.question ?: return
        if (state.currentCard != null || state.feedback != null || !state.canCheck) return

        val answer = when (question.type) {
            QuestionType.CHOICE -> Answer.Choice(state.selectedChoice ?: return)
            QuestionType.ORDER -> Answer.Order(state.pickedOrder.map { state.orderPool[it] })
        }
        val correct = question.isCorrect(answer)
        attempts += AttemptRecord(questionId = question.id, topic = question.topic, correct = correct)
        _uiState.update {
            it.copy(
                feedback = Feedback(
                    correct = correct,
                    correctAnswer = question.correctAnswerText(),
                    explanation = question.explanation,
                ),
            )
        }
        viewModelScope.launch {
            if (settingsRepository.settings.first().soundOn) soundPlayer.play(correct)
        }
    }

    /** Moves on after feedback: to the next question, or to saving the result at the end. */
    fun next() {
        val feedback = _uiState.value.feedback ?: return
        val updated = session?.answer(feedback.correct) ?: return
        session = updated
        if (updated.isFinished) finish(updated) else showCurrentQuestion()
    }

    private suspend fun loadLessonQuestions(): List<Question> {
        lessonTitle = curriculumRepository.getLesson(lessonId)?.title.orEmpty()
        _uiState.update { it.copy(title = lessonTitle) }
        return curriculumRepository.getQuestions(lessonId)
    }

    private suspend fun isCompletedBefore(): Boolean =
        curriculumRepository.observeLessons().first().any { it.lesson.id == lessonId && it.isCompleted }

    private suspend fun loadBonusQuestions(): List<Question> {
        lessonTitle = TriviaQuestions.TITLE
        val difficulty = settingsRepository.settings.first().difficulty
        return when (val round = triviaRepository.loadBonusRound(difficulty)) {
            is BonusRound.Ready -> {
                _uiState.update { it.copy(fromCache = round.fromCache) }
                round.questions
            }
            BonusRound.Unavailable -> emptyList()
        }
    }

    private fun showCurrentQuestion() {
        val current = session ?: return
        val question = current.current ?: return
        _uiState.update {
            it.copy(
                isLoading = false,
                loadFailed = false,
                question = question,
                isReviewingMistake = current.isReviewingMistake,
                selectedChoice = null,
                orderPool = if (question.type == QuestionType.ORDER) question.scrambledLines() else emptyList(),
                pickedOrder = emptyList(),
                feedback = null,
                mastered = current.mastered,
                total = current.total,
            )
        }
    }

    private fun finish(finished: QuizSession) {
        val result = SessionResult(
            lessonId = lessonId,
            lessonTitle = lessonTitle,
            total = finished.total,
            firstTryCorrect = finished.firstTryCorrect,
            stars = Scoring.stars(finished.firstTryCorrect, finished.total),
            xp = Scoring.xp(finished.firstTryCorrect, finished.total),
            isBonus = isBonus,
        )
        // Clearing the feedback first makes a second tap on "Continue" a no-op while saving.
        _uiState.update { it.copy(feedback = null, mastered = finished.mastered, isLoading = true) }
        viewModelScope.launch {
            val saved = result.copy(firstCompletion = !isBonus && !isCompletedBefore())
            val saveAttempts = settingsRepository.settings.first().detailedHistory
            progressRepository.recordSession(saved, attempts.toList(), saveAttempts)
            _uiState.update { it.copy(isLoading = false, result = saved) }
        }
    }
}
