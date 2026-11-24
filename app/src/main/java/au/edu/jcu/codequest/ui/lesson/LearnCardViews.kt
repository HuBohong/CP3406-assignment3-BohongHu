package au.edu.jcu.codequest.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.learn.FixTheBug
import au.edu.jcu.codequest.domain.learn.LearnCard
import au.edu.jcu.codequest.domain.learn.Playground
import au.edu.jcu.codequest.domain.learn.StepThrough
import au.edu.jcu.codequest.domain.learn.Variable
import au.edu.jcu.codequest.ui.components.ByteMascot
import au.edu.jcu.codequest.ui.components.ByteMood
import au.edu.jcu.codequest.ui.components.ChunkyButton
import au.edu.jcu.codequest.ui.components.ChunkySurface
import au.edu.jcu.codequest.ui.components.CodeEditor
import au.edu.jcu.codequest.ui.components.CodeQuestIcons
import au.edu.jcu.codequest.ui.components.CodeTextStyle
import au.edu.jcu.codequest.ui.components.highlightKotlin
import au.edu.jcu.codequest.ui.theme.ChunkyColors
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme
import kotlin.math.roundToInt

/**
 * Shows one teaching card. [onReady] is called once the learner has done what the card asks
 * (changed a value, stepped to the end, found the bug), which unlocks Continue.
 */
@Composable
fun LearnCardContent(card: LearnCard, onReady: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CardHeader(card)
        when (card) {
            is Playground -> PlaygroundCard(card, onReady)
            is StepThrough -> StepThroughCard(card, onReady)
            is FixTheBug -> FixTheBugCard(card, onReady)
        }
    }
}

@Composable
private fun CardHeader(card: LearnCard) {
    val (label, icon, tint) = when (card) {
        is Playground -> Triple(stringResource(R.string.learn_try_it), Icons.Filled.Star, MaterialTheme.colorScheme.primary)
        is StepThrough -> Triple(stringResource(R.string.learn_watch_it), Icons.Filled.PlayArrow, MaterialTheme.colorScheme.primary)
        is FixTheBug -> Triple(stringResource(R.string.learn_fix_bug), Icons.Filled.Warning, MaterialTheme.colorScheme.error)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KindChip(label = label, icon = icon, tint = tint)
        Text(card.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(card.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KindChip(label: String, icon: ImageVector, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

// =====================================================================================
// Try it
// =====================================================================================

@Composable
private fun PlaygroundCard(card: Playground, onReady: () -> Unit) {
    // Slot choices are kept as option indexes, saved as text so they survive rotation.
    var encoded by rememberSaveable(card.id) { mutableStateOf(card.slots.joinToString(",") { it.initial.toString() }) }
    val indices = encoded.split(",").map(String::toInt)
    var prediction by rememberSaveable(card.id) { mutableIntStateOf(-1) }
    var changed by rememberSaveable(card.id) { mutableStateOf(false) }

    val predicted = card.prediction == null || prediction >= 0
    val values = card.valuesAt(indices)
    val result = remember(encoded) { card.evaluate(values) }
    val goalMet = card.isGoalMet(values)

    fun setIndex(slot: Int, index: Int) {
        val options = card.slots[slot].options
        val next = indices.toMutableList().also { it[slot] = index.coerceIn(0, options.lastIndex) }
        if (next != indices) {
            encoded = next.joinToString(",")
            changed = true
        }
    }

    LaunchedEffect(predicted, changed) {
        if (predicted && changed) onReady()
    }

    TemplateEditor(
        card = card,
        indices = indices,
        highlightedLine = if (predicted) result.highlightedLine else null,
        enabled = predicted,
        onCycleSlot = { slot -> setIndex(slot, (indices[slot] + 1) % card.slots[slot].options.size) },
    )

    card.prediction?.let { question ->
        PredictionBlock(
            question = question.question,
            options = question.options,
            correctIndex = question.correctIndex,
            chosen = prediction.takeIf { it >= 0 },
            onChoose = { prediction = it },
        )
    }

    if (predicted) {
        SceneView(result.scene)
        OutputConsole(lines = result.output, error = result.error)
        result.note?.let { InfoNote(text = it) }
        card.slots.forEachIndexed { slot, spec ->
            SlotStepper(
                name = spec.name,
                value = values[slot],
                canDecrease = indices[slot] > 0,
                canIncrease = indices[slot] < spec.options.lastIndex,
                onDecrease = { setIndex(slot, indices[slot] - 1) },
                onIncrease = { setIndex(slot, indices[slot] + 1) },
            )
        }
        card.goal?.let { GoalLine(goal = it, met = goalMet) }
        if (!changed) {
            Text(
                text = stringResource(R.string.learn_change_value),
                style = MaterialTheme.typography.bodyMedium,
                color = CodeQuestTheme.colors.onXpSurface,
            )
        }
    }
}

/** Dark code editor in which each `{n}` placeholder is a tappable amber value chip. */
@Composable
private fun TemplateEditor(
    card: Playground,
    indices: List<Int>,
    highlightedLine: Int?,
    enabled: Boolean,
    onCycleSlot: (Int) -> Unit,
) {
    val colors = CodeQuestTheme.colors.editor
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.background)
            .padding(vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .widthIn(min = maxWidth)
                .width(IntrinsicSize.Max),
        ) {
            card.code.forEachIndexed { index, line ->
                val current = highlightedLine == index + 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (current) colors.currentLine else Color.Transparent)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .heightIn(min = 24.dp)
                            .background(if (current) colors.currentLineMarker else Color.Transparent),
                    )
                    Text(
                        text = (index + 1).toString(),
                        style = CodeTextStyle,
                        color = colors.lineNumber,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(30.dp),
                    )
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        var cursor = 0
                        Playground.PLACEHOLDER.findAll(line).forEach { match ->
                            val before = line.substring(cursor, match.range.first)
                            if (before.isNotEmpty()) {
                                Text(highlightKotlin(before, colors), style = CodeTextStyle, color = colors.text, softWrap = false)
                            }
                            val slot = match.groupValues[1].toInt()
                            val value = card.slots[slot].options[indices[slot]]
                            // Spaces around a value such as " until " belong to the code, not the chip.
                            if (value.startsWith(" ")) Text(" ", style = CodeTextStyle)
                            SlotChip(
                                value = value.trim(),
                                name = card.slots[slot].name,
                                enabled = enabled,
                                onClick = { onCycleSlot(slot) },
                            )
                            if (value.endsWith(" ")) Text(" ", style = CodeTextStyle)
                            cursor = match.range.last + 1
                        }
                        val rest = line.substring(cursor)
                        if (rest.isNotEmpty()) {
                            Text(highlightKotlin(rest, colors), style = CodeTextStyle, color = colors.text, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotChip(value: String, name: String, enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.learn_slot_cycle, name)
    Text(
        text = value,
        style = CodeTextStyle.copy(fontWeight = FontWeight.Bold),
        color = Color(0xFFFFD27A),
        softWrap = false,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x29FFB31A))
            .border(2.dp, if (enabled) Color(0xFFFFB31A) else Color(0xFF6F6990), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 6.dp),
    )
}

@Composable
private fun SlotStepper(
    name: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val extra = CodeQuestTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(2.dp, extra.cardEdge, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        StepButton("−", stringResource(R.string.learn_decrease, name), canDecrease, onDecrease)
        Text(
            value.trim(),
            style = CodeTextStyle.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 48.dp),
        )
        StepButton("+", stringResource(R.string.learn_increase, name), canIncrease, onIncrease)
    }
}

@Composable
private fun StepButton(symbol: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    ChunkySurface(
        colors = ChunkyColors(face = scheme.primaryContainer, edge = scheme.inversePrimary, content = scheme.onPrimaryContainer),
        shape = RoundedCornerShape(12.dp),
        depth = 3.dp,
        enabled = enabled,
        onClickLabel = label,
        onClick = onClick,
        modifier = Modifier
            .size(44.dp, 47.dp)
            .semantics { contentDescription = label },
    ) {
        Text(symbol, style = MaterialTheme.typography.titleLarge, color = if (enabled) scheme.onPrimaryContainer else scheme.outline)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PredictionBlock(
    question: String,
    options: List<String>,
    correctIndex: Int,
    chosen: Int?,
    onChoose: (Int) -> Unit,
) {
    val extra = CodeQuestTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.learn_predict_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
        Text(question, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, option ->
                val colors = when {
                    chosen == null -> extra.neutralButton
                    index == correctIndex -> extra.successButton
                    index == chosen -> extra.dangerButton
                    else -> extra.neutralButton
                }
                ChunkySurface(
                    colors = colors,
                    shape = RoundedCornerShape(12.dp),
                    depth = 4.dp,
                    borderWidth = if (colors.border != colors.face) 2.dp else 0.dp,
                    enabled = chosen == null,
                    onClick = { onChoose(index) },
                ) {
                    Text(
                        option,
                        style = CodeTextStyle.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
        if (chosen != null) {
            Text(
                text = stringResource(if (chosen == correctIndex) R.string.learn_predict_correct else R.string.learn_predict_wrong),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun GoalLine(goal: String, met: Boolean) {
    val extra = CodeQuestTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (met) extra.successContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (met) Icons.Filled.Check else CodeQuestIcons.Flag,
            contentDescription = null,
            tint = if (met) extra.success else extra.onXpSurface,
            modifier = Modifier.size(22.dp),
        )
        Column {
            Text(
                stringResource(if (met) R.string.learn_goal_done else R.string.learn_goal_label),
                style = MaterialTheme.typography.labelLarge,
                color = if (met) extra.success else extra.onXpSurface,
            )
            Text(goal, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// =====================================================================================
// Watch it
// =====================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepThroughCard(card: StepThrough, onReady: () -> Unit) {
    var index by rememberSaveable(card.id) { mutableIntStateOf(0) }
    var reachedEnd by rememberSaveable(card.id) { mutableStateOf(false) }
    val last = card.steps.lastIndex
    val step = card.steps[index]
    val scene = card.steps.take(index + 1).lastOrNull { it.scene != null }?.scene

    LaunchedEffect(index) {
        if (index == last) reachedEnd = true
    }
    LaunchedEffect(reachedEnd) {
        if (reachedEnd) onReady()
    }

    CodeEditor(code = card.code.joinToString("\n"), highlightedLine = step.line)

    Text(stringResource(R.string.learn_variables), style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        step.variables.forEach { VariableCard(it) }
    }
    scene?.let { SceneView(it) }
    OutputConsole(lines = step.output, error = null)

    Text(
        text = stringResource(R.string.learn_step_of, index + 1, card.steps.size),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (last > 0) {
        Slider(
            value = index.toFloat(),
            onValueChange = { index = it.roundToInt().coerceIn(0, last) },
            valueRange = 0f..last.toFloat(),
            steps = (last - 1).coerceAtLeast(0),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ChunkyButton(
            onClick = { index = (index - 1).coerceAtLeast(0) },
            enabled = index > 0,
            colors = CodeQuestTheme.colors.neutralButton,
            height = 48.dp,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.learn_back))
        }
        ChunkyButton(
            onClick = { index = if (index < last) index + 1 else 0 },
            height = 48.dp,
            modifier = Modifier.weight(2f),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text(
                if (index < last) {
                    stringResource(R.string.learn_run_line, card.steps[index + 1].line)
                } else {
                    stringResource(R.string.learn_restart)
                },
            )
        }
    }
    if (!reachedEnd) {
        Text(stringResource(R.string.learn_watch_hint), style = MaterialTheme.typography.bodyMedium, color = CodeQuestTheme.colors.onXpSurface)
    }
}

/** One variable: its name, val (locked) or var, its value, and the old value when it just changed. */
@Composable
private fun VariableCard(variable: Variable) {
    val extra = CodeQuestTheme.colors
    val changed = variable.previous != null
    Column(
        modifier = Modifier
            .widthIn(min = 120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (changed) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(2.dp, if (changed) extra.xp else extra.cardEdge, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(variable.name, style = CodeTextStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (variable.isVal) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (variable.isVal) Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(11.dp))
                Text(if (variable.isVal) "val" else "var", style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            variable.previous?.let {
                Text(
                    it,
                    style = CodeTextStyle,
                    color = MaterialTheme.colorScheme.outline,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.semantics { contentDescription = "was $it" },
                )
            }
            Text(variable.value, style = MaterialTheme.typography.titleLarge.copy(fontFamily = CodeTextStyle.fontFamily))
        }
    }
}

// =====================================================================================
// Fix the bug
// =====================================================================================

@Composable
private fun FixTheBugCard(card: FixTheBug, onReady: () -> Unit) {
    var found by rememberSaveable(card.id) { mutableStateOf(false) }
    var wrongLine by rememberSaveable(card.id) { mutableIntStateOf(0) }
    val extra = CodeQuestTheme.colors
    val editor = extra.editor

    LaunchedEffect(found) {
        if (found) onReady()
    }

    // The problem, as the compiler or the run reports it.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (found) extra.successContainer else MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (found) Icons.Filled.Check else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (found) extra.success else MaterialTheme.colorScheme.error,
        )
        Text(
            text = if (found) stringResource(R.string.learn_fixed) else card.problem,
            style = CodeTextStyle.copy(fontWeight = FontWeight.Bold),
            color = if (found) extra.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(editor.background)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val lines = if (found) card.fixedCode else card.code
        lines.forEachIndexed { index, line ->
            val number = index + 1
            val isFix = found && number == card.buggyLine
            val isWrong = !found && number == wrongLine
            val clickLabel = stringResource(R.string.learn_fix_tap_label, number)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            isFix -> Color(0x4012A06D)
                            isWrong -> Color(0x40E5484D)
                            else -> Color.Transparent
                        },
                    )
                    .clickable(enabled = !found, role = Role.Button, onClickLabel = clickLabel) {
                        if (number == card.buggyLine) found = true else wrongLine = number
                    }
                    .heightIn(min = 44.dp)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = number.toString(),
                    style = CodeTextStyle,
                    color = editor.lineNumber,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(30.dp),
                )
                Text(
                    text = highlightKotlin(line, editor),
                    style = CodeTextStyle,
                    color = editor.text,
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .weight(1f),
                )
                if (isFix) Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF7EE0A8), modifier = Modifier.size(18.dp))
            }
        }
    }

    when {
        found -> {
            InfoNote(text = card.explanation, happy = true)
            OutputConsole(lines = card.fixedOutput, error = null)
        }
        wrongLine > 0 -> InfoNote(text = card.wrongLineHint)
        else -> Text(stringResource(R.string.learn_fix_hint), style = MaterialTheme.typography.bodyMedium, color = CodeQuestTheme.colors.onXpSurface)
    }
}

// =====================================================================================
// Shared pieces
// =====================================================================================

@Composable
private fun OutputConsole(lines: List<String>, error: String?) {
    val editor = CodeQuestTheme.colors.editor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF15112B))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(stringResource(R.string.learn_output), style = CodeTextStyle, color = editor.lineNumber)
        when {
            error != null -> Text(error, style = CodeTextStyle, color = Color(0xFFFF9AA5))
            lines.isEmpty() -> Text(stringResource(R.string.learn_nothing_printed), style = CodeTextStyle, color = editor.lineNumber)
            else -> lines.forEach { Text(it, style = CodeTextStyle, color = Color(0xFF9EF0C2)) }
        }
    }
}

@Composable
private fun InfoNote(text: String, happy: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ByteMascot(Modifier.size(34.dp), mood = if (happy) ByteMood.CHEERING else ByteMood.HAPPY)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}
