package au.edu.jcu.codequest.ui.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.model.Question
import au.edu.jcu.codequest.domain.model.QuestionType
import au.edu.jcu.codequest.domain.model.SessionResult
import au.edu.jcu.codequest.ui.components.CodeBlock
import au.edu.jcu.codequest.ui.components.LoadingBox
import au.edu.jcu.codequest.ui.components.contentWidth

object LessonTestTags {
    const val CHECK = "lesson_check"
    const val CONTINUE = "lesson_continue"
    const val FEEDBACK = "lesson_feedback"
    fun option(index: Int) = "lesson_option_$index"
    fun poolLine(index: Int) = "lesson_pool_line_$index"
}

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
) {
    val question = uiState.question
    Scaffold(
        topBar = { LessonTopBar(uiState = uiState, onClose = onClose) },
        bottomBar = {
            if (question != null && !uiState.isLoading) {
                LessonBottomBar(uiState = uiState, onCheck = onCheck, onContinue = onContinue)
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

@Composable
private fun LessonTopBar(uiState: LessonUiState, onClose: () -> Unit) {
    val progressDescription = stringResource(R.string.lesson_progress_description, uiState.mastered, uiState.total)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.lesson_close))
        }
        LinearProgressIndicator(
            progress = { uiState.progress },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .semantics { contentDescription = progressDescription },
        )
        Text(
            text = stringResource(R.string.lesson_progress_count, uiState.mastered, uiState.total),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 12.dp),
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.contentWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            question.code?.let { CodeBlock(code = it) }
            when (question.type) {
                QuestionType.CHOICE -> ChoiceOptions(
                    question = question,
                    selectedIndex = uiState.selectedChoice,
                    feedback = uiState.feedback,
                    monospace = !uiState.isBonus,
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
    monospace: Boolean,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        question.options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            OptionRow(
                text = option,
                isSelected = isSelected,
                // After checking, the right answer is always revealed; a wrong pick is marked too.
                showAsCorrect = feedback != null && index == question.correctIndex,
                showAsWrong = feedback != null && isSelected && !feedback.correct,
                enabled = feedback == null,
                monospace = monospace,
                onClick = { onSelect(index) },
                modifier = Modifier.testTag(LessonTestTags.option(index)),
            )
        }
    }
}

@Composable
private fun OptionRow(
    text: String,
    isSelected: Boolean,
    showAsCorrect: Boolean,
    showAsWrong: Boolean,
    enabled: Boolean,
    monospace: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = when {
        showAsCorrect -> colors.primaryContainer
        showAsWrong -> colors.errorContainer
        isSelected -> colors.secondaryContainer
        else -> colors.surface
    }
    val borderColor = when {
        showAsCorrect -> colors.primary
        showAsWrong -> colors.error
        isSelected -> colors.secondary
        else -> colors.outlineVariant
    }
    val shape = MaterialTheme.shapes.medium
    Surface(
        color = containerColor,
        shape = shape,
        border = BorderStroke(width = if (isSelected || showAsCorrect) 2.dp else 1.dp, color = borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = if (monospace) FontFamily.Monospace else null,
                modifier = Modifier.weight(1f),
            )
            // Icons back up the colours so the result never depends on colour alone.
            when {
                showAsCorrect -> Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.lesson_option_correct),
                    tint = colors.primary,
                )
                showAsWrong -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.lesson_option_wrong),
                    tint = colors.error,
                )
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.lesson_order_program),
            style = MaterialTheme.typography.labelLarge,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .heightIn(min = 72.dp)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (picked.isEmpty()) {
                    Text(
                        text = stringResource(R.string.lesson_order_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                picked.forEachIndexed { position, poolIndex ->
                    CodeLine(
                        text = pool.getOrElse(poolIndex) { "" },
                        number = position + 1,
                        enabled = enabled,
                        clickLabel = removeLabel,
                        onClick = { onRemove(position) },
                    )
                }
            }
        }
        val remaining = pool.indices.filter { it !in picked }
        if (remaining.isNotEmpty()) {
            Text(
                text = stringResource(R.string.lesson_order_available),
                style = MaterialTheme.typography.labelLarge,
            )
            remaining.forEach { poolIndex ->
                CodeLine(
                    text = pool[poolIndex],
                    number = null,
                    enabled = enabled,
                    clickLabel = addLabel,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = enabled, onClickLabel = clickLabel, role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (number != null) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = text,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LessonBottomBar(uiState: LessonUiState, onCheck: () -> Unit, onContinue: () -> Unit) {
    val feedback = uiState.feedback
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (feedback != null) FeedbackPanel(feedback)
            Button(
                onClick = if (feedback == null) onCheck else onContinue,
                enabled = feedback != null || uiState.canCheck,
                modifier = Modifier
                    .contentWidth()
                    .padding(16.dp)
                    .heightIn(min = 52.dp)
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
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (feedback.correct) colors.primaryContainer else colors.errorContainer,
        contentColor = if (feedback.correct) colors.onPrimaryContainer else colors.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LessonTestTags.FEEDBACK)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 280.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (feedback.correct) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(
                        if (feedback.correct) R.string.lesson_feedback_correct else R.string.lesson_feedback_wrong,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (!feedback.correct) {
                Text(
                    text = stringResource(R.string.lesson_feedback_answer_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = feedback.correctAnswer,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(text = feedback.explanation, style = MaterialTheme.typography.bodyMedium)
            if (!feedback.correct) {
                Text(
                    text = stringResource(R.string.lesson_feedback_retry_note),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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
        Text(
            text = stringResource(if (isBonus) R.string.lesson_bonus_failed_title else R.string.lesson_failed_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(if (isBonus) R.string.lesson_bonus_failed_body else R.string.lesson_failed_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.lesson_retry)) }
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
