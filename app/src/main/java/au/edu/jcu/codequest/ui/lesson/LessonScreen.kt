package au.edu.jcu.codequest.ui.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.model.Question
import au.edu.jcu.codequest.domain.model.QuestionType
import au.edu.jcu.codequest.domain.model.SessionResult
import au.edu.jcu.codequest.ui.components.ByteMascot
import au.edu.jcu.codequest.ui.components.ByteMood
import au.edu.jcu.codequest.ui.components.ChunkyButton
import au.edu.jcu.codequest.ui.components.ChunkyProgressBar
import au.edu.jcu.codequest.ui.components.ChunkySurface
import au.edu.jcu.codequest.ui.components.CodeEditor
import au.edu.jcu.codequest.ui.components.CodeTextStyle
import au.edu.jcu.codequest.ui.components.LoadingBox
import au.edu.jcu.codequest.ui.components.contentWidth
import au.edu.jcu.codequest.ui.theme.ChunkyColors
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme

object LessonTestTags {
    const val CARD_CONTINUE = "lesson_card_continue"
    const val CHECK = "lesson_check"
    const val CONTINUE = "lesson_continue"
    const val FEEDBACK = "lesson_feedback"
    fun option(index: Int) = "lesson_option_$index"
    fun poolLine(index: Int) = "lesson_pool_line_$index"
}

/** Options shorter than this are laid out two per row, like answer tiles in a game. */
private const val SHORT_OPTION_LENGTH = 14

@Composable
fun LessonRoute(
    onExit: () -> Unit,
    onFinished: (SessionResult) -> Unit,
    viewModel: LessonViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    // Leaving mid-lesson loses the answers so far, so ask first. Nothing to lose otherwise.
    val lessonInProgress = uiState.question != null && uiState.result == null
    BackHandler(enabled = lessonInProgress) { showExitDialog = true }

    LaunchedEffect(uiState.result) {
        uiState.result?.let(onFinished)
    }

    LessonScreen(
        uiState = uiState,
        onSelectChoice = viewModel::selectChoice,
        onPickLine = viewModel::pickLine,
        onRemoveLine = viewModel::removeLine,
        onCheck = viewModel::check,
        onContinue = viewModel::next,
        onRetryLoad = viewModel::load,
        onClose = { if (lessonInProgress) showExitDialog = true else onExit() },
        onCardReady = viewModel::markCardReady,
        onNextCard = viewModel::nextCard,
    )

    if (showExitDialog) {
        ExitDialog(
            onStay = { showExitDialog = false },
            onLeave = {
                showExitDialog = false
                onExit()
            },
        )
    }
}

/** The main learning interaction. Stateless: everything comes in through [uiState]. */
@Composable
fun LessonScreen(
    uiState: LessonUiState,
    onSelectChoice: (Int) -> Unit,
    onPickLine: (Int) -> Unit,
    onRemoveLine: (Int) -> Unit,
    onCheck: () -> Unit,
    onContinue: () -> Unit,
    onRetryLoad: () -> Unit,
    onClose: () -> Unit,
    onCardReady: () -> Unit = {},
    onNextCard: () -> Unit = {},
) {
    val question = uiState.question
    val card = uiState.currentCard
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { LessonTopBar(uiState = uiState, onClose = onClose) },
        bottomBar = {
            when {
                uiState.isLoading || question == null -> Unit
                card != null -> CardBottomBar(ready = uiState.cardReady, onNext = onNextCard)
                else -> LessonBottomBar(uiState = uiState, onCheck = onCheck, onContinue = onContinue)
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingBox(Modifier.padding(innerPadding))
            uiState.loadFailed -> LoadFailed(
                isBonus = uiState.isBonus,
                onRetry = onRetryLoad,
                onClose = onClose,
                modifier = Modifier.padding(innerPadding),
            )
            card != null -> CardContent(
                uiState = uiState,
                onCardReady = onCardReady,
                modifier = Modifier.padding(innerPadding),
            )
            question != null -> QuestionContent(
                uiState = uiState,
                question = question,
                onSelectChoice = onSelectChoice,
                onPickLine = onPickLine,
                onRemoveLine = onRemoveLine,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

/** A teaching card, scrolled as one page. The key resets scroll position for each new card. */
@Composable
private fun CardContent(uiState: LessonUiState, onCardReady: () -> Unit, modifier: Modifier = Modifier) {
    val card = uiState.currentCard ?: return
    androidx.compose.runtime.key(card.id) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.title.isNotBlank()) {
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .contentWidth()
                        .padding(bottom = 10.dp),
                )
            }
            LearnCardContent(card = card, onReady = onCardReady, modifier = Modifier.contentWidth())
        }
    }
}

@Composable
private fun CardBottomBar(ready: Boolean, onNext: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChunkyButton(
                onClick = onNext,
                enabled = ready,
                modifier = Modifier
                    .contentWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                    .testTag(LessonTestTags.CARD_CONTINUE),
            ) {
                Text(stringResource(R.string.lesson_continue))
            }
        }
    }
}

@Composable
private fun LessonTopBar(uiState: LessonUiState, onClose: () -> Unit) {
    val progressDescription = stringResource(R.string.lesson_progress_description, uiState.stepsDone, uiState.stepsTotal)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.lesson_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ChunkyProgressBar(
            progress = uiState.progress,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
                .semantics { contentDescription = progressDescription },
        )
        Text(
            text = stringResource(R.string.lesson_progress_count, uiState.stepsDone, uiState.stepsTotal),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun QuestionContent(
    uiState: LessonUiState,
    question: Question,
    onSelectChoice: (Int) -> Unit,
    onPickLine: (Int) -> Unit,
    onRemoveLine: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.contentWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val label = when {
                uiState.isReviewingMistake -> stringResource(R.string.lesson_review_label)
                uiState.isBonus && uiState.fromCache -> stringResource(R.string.lesson_bonus_offline_label)
                uiState.isBonus -> stringResource(R.string.lesson_bonus_label)
                else -> uiState.title
            }
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (uiState.isReviewingMistake) {
                        CodeQuestTheme.colors.onXpSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            question.code?.let { CodeEditor(code = it) }
            when (question.type) {
                QuestionType.CHOICE -> ChoiceOptions(
                    question = question,
                    selectedIndex = uiState.selectedChoice,
                    feedback = uiState.feedback,
                    codeAnswers = !uiState.isBonus,
                    onSelect = onSelectChoice,
                )
                QuestionType.ORDER -> OrderBuilder(
                    pool = uiState.orderPool,
                    picked = uiState.pickedOrder,
                    enabled = uiState.feedback == null,
                    onPick = onPickLine,
                    onRemove = onRemoveLine,
                )
            }
        }
    }
}

@Composable
private fun ChoiceOptions(
    question: Question,
    selectedIndex: Int?,
    feedback: Feedback?,
    codeAnswers: Boolean,
    onSelect: (Int) -> Unit,
) {
    val asGrid = question.options.size == 4 && question.options.all { it.length <= SHORT_OPTION_LENGTH }
    val rows = if (asGrid) question.options.indices.chunked(2) else question.options.indices.map { listOf(it) }
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { index ->
                    val isSelected = index == selectedIndex
                    OptionTile(
                        text = question.options[index],
                        isSelected = isSelected,
                        // After checking, the right answer is always revealed; a wrong pick is marked too.
                        showAsCorrect = feedback != null && index == question.correctIndex,
                        showAsWrong = feedback != null && isSelected && !feedback.correct,
                        dimmed = feedback != null && index != question.correctIndex && !isSelected,
                        enabled = feedback == null,
                        code = codeAnswers,
                        centred = asGrid,
                        onClick = { onSelect(index) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(LessonTestTags.option(index)),
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionTile(
    text: String,
    isSelected: Boolean,
    showAsCorrect: Boolean,
    showAsWrong: Boolean,
    dimmed: Boolean,
    enabled: Boolean,
    code: Boolean,
    centred: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val extra = CodeQuestTheme.colors
    val colors = when {
        showAsCorrect -> ChunkyColors(
            face = extra.successContainer,
            edge = extra.successButton.face,
            content = extra.success,
            border = extra.successButton.face,
        )
        showAsWrong -> ChunkyColors(
            face = scheme.errorContainer,
            edge = scheme.error,
            content = scheme.onErrorContainer,
            border = scheme.error,
        )
        isSelected -> ChunkyColors(
            face = scheme.primaryContainer,
            edge = scheme.primary,
            content = scheme.onPrimaryContainer,
            border = scheme.primary,
        )
        else -> ChunkyColors(
            face = scheme.surfaceContainerLowest,
            edge = extra.cardEdge,
            content = scheme.onSurface,
            border = extra.cardEdge,
        )
    }
    ChunkySurface(
        colors = colors,
        borderWidth = 2.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .heightIn(min = 64.dp)
            .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, if (centred) Alignment.CenterHorizontally else Alignment.Start),
        ) {
            // Icons back up the colours so the result never depends on colour alone.
            when {
                showAsCorrect -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.lesson_option_correct),
                    modifier = Modifier.size(20.dp),
                )
                showAsWrong -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.lesson_option_wrong),
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = text,
                style = if (code) CodeTextStyle.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyLarge,
                textAlign = if (centred) TextAlign.Center else TextAlign.Start,
                color = if (dimmed) colors.content.copy(alpha = 0.55f) else colors.content,
                modifier = if (centred) Modifier else Modifier.weight(1f, fill = false),
            )
        }
    }
}

/** Parsons problem: tap lines in the pool to build the program, tap a placed line to remove it. */
@Composable
private fun OrderBuilder(
    pool: List<String>,
    picked: List<Int>,
    enabled: Boolean,
    onPick: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val addLabel = stringResource(R.string.lesson_order_add)
    val removeLabel = stringResource(R.string.lesson_order_remove)
    val editor = CodeQuestTheme.colors.editor
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.lesson_order_program),
            style = MaterialTheme.typography.titleSmall,
        )
        Surface(
            color = editor.background,
            contentColor = editor.text,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .heightIn(min = 72.dp)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (picked.isEmpty()) {
                    Text(
                        text = stringResource(R.string.lesson_order_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = editor.lineNumber,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                picked.forEachIndexed { position, poolIndex ->
                    CodeLine(
                        text = pool.getOrElse(poolIndex) { "" },
                        number = position + 1,
                        enabled = enabled,
                        clickLabel = removeLabel,
                        onDark = true,
                        onClick = { onRemove(position) },
                    )
                }
            }
        }
        val remaining = pool.indices.filter { it !in picked }
        if (remaining.isNotEmpty()) {
            Text(
                text = stringResource(R.string.lesson_order_available),
                style = MaterialTheme.typography.titleSmall,
            )
            remaining.forEach { poolIndex ->
                CodeLine(
                    text = pool[poolIndex],
                    number = null,
                    enabled = enabled,
                    clickLabel = addLabel,
                    onDark = false,
                    onClick = { onPick(poolIndex) },
                    modifier = Modifier.testTag(LessonTestTags.poolLine(poolIndex)),
                )
            }
        }
    }
}

@Composable
private fun CodeLine(
    text: String,
    number: Int?,
    enabled: Boolean,
    clickLabel: String,
    onDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editor = CodeQuestTheme.colors.editor
    val colors = if (onDark) {
        ChunkyColors(face = Color(0xFF2E2850), edge = Color(0xFF16122B), content = editor.text)
    } else {
        ChunkyColors(
            face = MaterialTheme.colorScheme.surfaceContainerLowest,
            edge = CodeQuestTheme.colors.cardEdge,
            content = MaterialTheme.colorScheme.onSurface,
            border = CodeQuestTheme.colors.cardEdge,
        )
    }
    ChunkySurface(
        colors = colors,
        shape = RoundedCornerShape(12.dp),
        depth = 4.dp,
        borderWidth = if (onDark) 0.dp else 2.dp,
        enabled = enabled,
        onClickLabel = clickLabel,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (number != null) {
                Text(
                    text = number.toString(),
                    style = CodeTextStyle,
                    color = editor.lineNumber,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(18.dp),
                )
            }
            Text(text = text, style = CodeTextStyle, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LessonBottomBar(uiState: LessonUiState, onCheck: () -> Unit, onContinue: () -> Unit) {
    val feedback = uiState.feedback
    val extra = CodeQuestTheme.colors
    val sheetColor = when {
        feedback == null -> MaterialTheme.colorScheme.background
        feedback.correct -> extra.successContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    Surface(color = sheetColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = feedback != null,
                enter = slideInVertically { it / 2 },
            ) {
                if (feedback != null) FeedbackPanel(feedback)
            }
            val buttonColors = when {
                feedback == null -> extra.primaryButton
                feedback.correct -> extra.successButton
                else -> extra.dangerButton
            }
            ChunkyButton(
                onClick = if (feedback == null) onCheck else onContinue,
                enabled = feedback != null || uiState.canCheck,
                colors = buttonColors,
                modifier = Modifier
                    .contentWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                    .testTag(if (feedback == null) LessonTestTags.CHECK else LessonTestTags.CONTINUE),
            ) {
                Text(stringResource(if (feedback == null) R.string.lesson_check else R.string.lesson_continue))
            }
        }
    }
}

/** Explains the answer. Announced by TalkBack as soon as it appears (polite live region). */
@Composable
private fun FeedbackPanel(feedback: Feedback) {
    val extra = CodeQuestTheme.colors
    val textColor = if (feedback.correct) extra.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
    val titleColor = if (feedback.correct) extra.success else MaterialTheme.colorScheme.error
    Column(
        modifier = Modifier
            .contentWidth()
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 18.dp)
            .testTag(LessonTestTags.FEEDBACK)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ByteMascot(
                modifier = Modifier.size(44.dp),
                mood = if (feedback.correct) ByteMood.CHEERING else ByteMood.WORRIED,
            )
            Text(
                text = stringResource(
                    if (feedback.correct) R.string.lesson_feedback_correct else R.string.lesson_feedback_wrong,
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = titleColor,
            )
        }
        if (!feedback.correct) {
            Text(
                text = stringResource(R.string.lesson_feedback_answer_label),
                style = MaterialTheme.typography.titleSmall,
                color = textColor,
            )
            Text(
                text = feedback.correctAnswer,
                style = CodeTextStyle.copy(fontWeight = FontWeight.Bold),
                color = textColor,
            )
        }
        Text(text = feedback.explanation, style = MaterialTheme.typography.bodyLarge, color = textColor)
        if (!feedback.correct) {
            Text(
                text = stringResource(R.string.lesson_feedback_retry_note),
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
        }
    }
}

@Composable
private fun LoadFailed(
    isBonus: Boolean,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ByteMascot(Modifier.size(96.dp), mood = ByteMood.WORRIED)
        Text(
            text = stringResource(if (isBonus) R.string.lesson_bonus_failed_title else R.string.lesson_failed_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(if (isBonus) R.string.lesson_bonus_failed_body else R.string.lesson_failed_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.contentWidth()) {
            ChunkyButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.lesson_retry))
            }
        }
        TextButton(onClick = onClose) { Text(stringResource(R.string.lesson_back_to_path)) }
    }
}

/** Both actions are plain text buttons: staying and leaving are presented as equal choices. */
@Composable
private fun ExitDialog(onStay: () -> Unit, onLeave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onStay,
        title = { Text(stringResource(R.string.lesson_exit_title)) },
        text = { Text(stringResource(R.string.lesson_exit_body)) },
        confirmButton = { TextButton(onClick = onStay) { Text(stringResource(R.string.lesson_exit_stay)) } },
        dismissButton = { TextButton(onClick = onLeave) { Text(stringResource(R.string.lesson_exit_leave)) } },
    )
}
