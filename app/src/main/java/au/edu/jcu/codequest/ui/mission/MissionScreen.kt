package au.edu.jcu.codequest.ui.mission

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.mission.Block
import au.edu.jcu.codequest.domain.mission.BossState
import au.edu.jcu.codequest.domain.mission.CommandBlock
import au.edu.jcu.codequest.domain.mission.Condition
import au.edu.jcu.codequest.domain.mission.FailureReason
import au.edu.jcu.codequest.domain.mission.GridState
import au.edu.jcu.codequest.domain.mission.Heading
import au.edu.jcu.codequest.domain.mission.IfBlock
import au.edu.jcu.codequest.domain.mission.Mission
import au.edu.jcu.codequest.domain.mission.MissionKind
import au.edu.jcu.codequest.domain.mission.MissionOutcome
import au.edu.jcu.codequest.domain.mission.MissionState
import au.edu.jcu.codequest.domain.mission.PaletteBlock
import au.edu.jcu.codequest.domain.mission.RepeatBlock
import au.edu.jcu.codequest.domain.mission.Slot
import au.edu.jcu.codequest.domain.mission.WhileBlock
import au.edu.jcu.codequest.ui.components.ByteMascot
import au.edu.jcu.codequest.ui.components.ByteMood
import au.edu.jcu.codequest.ui.components.ChunkyButton
import au.edu.jcu.codequest.ui.components.ChunkySurface
import au.edu.jcu.codequest.ui.components.CodeQuestIcons
import au.edu.jcu.codequest.ui.components.LightSystemBarIcons
import au.edu.jcu.codequest.ui.components.SpaceBackground
import au.edu.jcu.codequest.ui.components.contentWidth
import au.edu.jcu.codequest.ui.theme.ChunkyColors
import au.edu.jcu.codequest.ui.theme.CodeFont
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme

object MissionTestTags {
    const val RUN = "mission_run"
    const val STEP = "mission_step"
    const val RESULT = "mission_result"
    fun palette(label: String) = "mission_palette_$label"
}

/** Colours for the three families of block, matching the toolbox chips. */
private data class BlockStyle(val tint: Color, val bar: Color, val text: Color)

private val ActionStyle = BlockStyle(tint = Color(0xFFF1ECFF), bar = Color(0xFF7F52FF), text = Color(0xFF4C22C9))
private val LoopStyle = BlockStyle(tint = Color(0xFFFFF1E3), bar = Color(0xFFF08A24), text = Color(0xFF8A4300))
private val DecisionStyle = BlockStyle(tint = Color(0xFFE1F6EE), bar = Color(0xFF12A06D), text = Color(0xFF0B6141))

private fun styleFor(block: Block): BlockStyle = when (block) {
    is CommandBlock -> ActionStyle
    is RepeatBlock, is WhileBlock -> LoopStyle
    is IfBlock -> DecisionStyle
}

private fun styleFor(item: PaletteBlock): BlockStyle = when (item) {
    is PaletteBlock.Act -> ActionStyle
    PaletteBlock.Repeat, PaletteBlock.While -> LoopStyle
    PaletteBlock.If, PaletteBlock.IfElse -> DecisionStyle
}

private val BlockText = androidx.compose.ui.text.TextStyle(fontFamily = CodeFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)

/** Everything the program editor can ask the ViewModel to do. */
private data class EditorActions(
    val selectTarget: (Slot) -> Unit,
    val toggleSelected: (Int) -> Unit,
    val remove: (Int) -> Unit,
    val changeTimes: (Int, Int) -> Unit,
    val cycleSensor: (Int) -> Unit,
    val toggleNegated: (Int) -> Unit,
)

@Composable
fun MissionRoute(
    onExit: () -> Unit,
    viewModel: MissionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MissionScreen(
        uiState = uiState,
        onExit = onExit,
        onAddBlock = viewModel::addBlock,
        editorActions = EditorActions(
            selectTarget = viewModel::selectTarget,
            toggleSelected = viewModel::toggleSelected,
            remove = viewModel::removeBlock,
            changeTimes = viewModel::changeRepeatTimes,
            cycleSensor = viewModel::cycleSensor,
            toggleNegated = viewModel::toggleNegated,
        ),
        onRun = viewModel::run,
        onStep = viewModel::step,
        onBackToEditing = viewModel::backToEditing,
        onClear = viewModel::clearProgram,
        onToggleHint = viewModel::toggleHint,
    )
}

/**
 * The end-of-unit mission: the board on top, the learner's program in the middle, the toolbox
 * underneath. Tapping a toolbox block adds it where the highlighted "add here" slot is.
 */
@Composable
private fun MissionScreen(
    uiState: MissionUiState,
    onExit: () -> Unit,
    onAddBlock: (PaletteBlock) -> Unit,
    editorActions: EditorActions,
    onRun: () -> Unit,
    onStep: () -> Unit,
    onBackToEditing: () -> Unit,
    onClear: () -> Unit,
    onToggleHint: () -> Unit,
) {
    LightSystemBarIcons()
    val mission = uiState.mission
    Box(Modifier.fillMaxSize()) {
        SpaceBackground(Modifier.matchParentSize(), seed = 23)
        if (mission == null) {
            Text(
                text = stringResource(R.string.mission_not_found),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )
            return@Box
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MissionTopBar(mission = mission, state = uiState.boardState, onExit = onExit)
            Column(
                modifier = Modifier
                    .contentWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (val board = uiState.boardState) {
                    is GridState -> GridBoard(board, description = gridDescription(board))
                    is BossState -> BossArena(board, description = bossDescription(board))
                    null -> Unit
                }
                GoalLine(mission = mission, showHint = uiState.showHint, onToggleHint = onToggleHint)
                ProgramCard(
                    uiState = uiState,
                    actions = editorActions,
                    onClear = onClear,
                    modifier = Modifier.weight(1f),
                )
                Toolbox(mission = mission, enabled = uiState.isEditable, onAddBlock = onAddBlock)
            }
            Controls(
                uiState = uiState,
                onRun = onRun,
                onStep = onStep,
                onBackToEditing = onBackToEditing,
                modifier = Modifier
                    .contentWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        AnimatedVisibility(
            visible = uiState.outcome != null,
            enter = slideInVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            uiState.outcome?.let { outcome ->
                ResultPanel(
                    outcome = outcome,
                    hasHint = !uiState.showHint,
                    onBackToMap = onExit,
                    onEdit = onBackToEditing,
                    onShowHint = {
                        onToggleHint()
                        onBackToEditing()
                    },
                )
            }
        }
    }
}

@Composable
private fun MissionTopBar(mission: Mission, state: MissionState?, onExit: () -> Unit) {
    val space = CodeQuestTheme.colors.space
    Row(
        modifier = Modifier
            .contentWidth()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onExit) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.mission_close), tint = space.textSoft)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { heading() },
        ) {
            Text(
                text = stringResource(
                    if (mission.kind == MissionKind.BOSS) R.string.mission_boss_label else R.string.mission_unit_label,
                    mission.unitIndex,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = space.textSoft,
            )
            Text(text = mission.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
        when (state) {
            is GridState -> StatusChip(
                text = stringResource(R.string.mission_count, state.collected, state.level.crystals.size),
                description = stringResource(R.string.mission_crystals_description, state.collected, state.level.crystals.size),
                color = Color(0xFF8FF5E4),
            )
            is BossState -> StatusChip(
                text = stringResource(R.string.mission_count, state.shields, state.level.shields),
                description = stringResource(R.string.mission_shields_description, state.shields, state.level.shields),
                color = Color(0xFFFFC94D),
            )
            null -> Unit
        }
    }
}

@Composable
private fun StatusChip(text: String, description: String, color: Color) {
    val space = CodeQuestTheme.colors.space
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(space.glass)
            .border(2.dp, space.glassBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(CodeQuestIcons.Flag, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun GoalLine(mission: Mission, showHint: Boolean, onToggleHint: () -> Unit) {
    val space = CodeQuestTheme.colors.space
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = mission.goal,
                style = MaterialTheme.typography.bodyMedium,
                color = space.textSoft,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggleHint) {
                Text(
                    stringResource(if (showHint) R.string.mission_hint_hide else R.string.mission_hint_show),
                    color = Color(0xFFFFC94D),
                )
            }
        }
        if (showHint) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(space.glass)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ByteMascot(Modifier.size(32.dp))
                Text(text = mission.hint, style = MaterialTheme.typography.bodyMedium, color = space.text)
            }
        }
    }
}

@Composable
private fun ProgramCard(
    uiState: MissionUiState,
    actions: EditorActions,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        contentColor = Color(0xFF1D1838),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.mission_program_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF5E5878),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                if (uiState.program.size > 0 && uiState.isEditable) {
                    TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(stringResource(R.string.mission_clear), color = Color(0xFF5A32D6))
                    }
                }
            }
            if (uiState.program.blocks.isEmpty()) {
                Text(
                    text = stringResource(R.string.mission_program_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5E5878),
                )
            }
            BlockList(blocks = uiState.program.blocks, slot = Slot.Root, ui = uiState, actions = actions)
        }
    }
}

@Composable
private fun BlockList(blocks: List<Block>, slot: Slot, ui: MissionUiState, actions: EditorActions) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block -> BlockView(block, ui, actions) }
        if (ui.isEditable) {
            SlotRow(isTarget = ui.target == slot, onClick = { actions.selectTarget(slot) })
        }
    }
}

@Composable
private fun BlockView(block: Block, ui: MissionUiState, actions: EditorActions) {
    val style = styleFor(block)
    when (block) {
        is CommandBlock -> BlockHeader(block, style, ui, actions) {
            Text(block.action.code, style = BlockText, color = style.text)
        }
        is RepeatBlock -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BlockHeader(block, style, ui, actions) {
                Text("repeat(", style = BlockText, color = style.text)
                ValueChip(text = block.times.toString(), style = style)
                Text(") {", style = BlockText, color = style.text)
            }
            Nested(style) { BlockList(block.body, Slot.Body(block.id), ui, actions) }
            ClosingBrace("}", style)
        }
        is IfBlock -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BlockHeader(block, style, ui, actions) {
                Text("if (", style = BlockText, color = style.text)
                ConditionChip(block.condition, style, enabled = ui.isEditable) { actions.cycleSensor(block.id) }
                Text(") {", style = BlockText, color = style.text)
            }
            Nested(style) { BlockList(block.body, Slot.Body(block.id), ui, actions) }
            block.elseBody?.let { elseBody ->
                ClosingBrace("} else {", style)
                Nested(style) { BlockList(elseBody, Slot.Else(block.id), ui, actions) }
            }
            ClosingBrace("}", style)
        }
        is WhileBlock -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BlockHeader(block, style, ui, actions) {
                Text("while (", style = BlockText, color = style.text)
                ConditionChip(block.condition, style, enabled = ui.isEditable) { actions.cycleSensor(block.id) }
                Text(") {", style = BlockText, color = style.text)
            }
            Nested(style) { BlockList(block.body, Slot.Body(block.id), ui, actions) }
            ClosingBrace("}", style)
        }
    }
}

/**
 * First line of a block. Tapping it shows its edit controls; the line that is running during a
 * replay gets an amber outline, matching the highlighted line in lesson code.
 */
@Composable
private fun BlockHeader(
    block: Block,
    style: BlockStyle,
    ui: MissionUiState,
    actions: EditorActions,
    content: @Composable () -> Unit,
) {
    val running = ui.runningBlockId == block.id
    val selected = ui.selectedBlockId == block.id && ui.isEditable
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(style.tint)
            .then(if (running) Modifier.border(3.dp, Color(0xFFFFB31A), shape) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable(enabled = ui.isEditable, onClickLabel = stringResource(R.string.mission_edit_block)) {
                    actions.toggleSelected(block.id)
                }
                .semantics { this.selected = selected },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(style.bar),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
        if (selected) BlockControls(block, style, actions)
    }
}

/** Edit controls for the selected block: count for repeat, "!" for conditions, and delete. */
@Composable
private fun BlockControls(block: Block, style: BlockStyle, actions: EditorActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (block) {
            is RepeatBlock -> {
                SmallControl(stringResource(R.string.mission_fewer), style) { actions.changeTimes(block.id, -1) }
                SmallControl(stringResource(R.string.mission_more), style) { actions.changeTimes(block.id, 1) }
            }
            is IfBlock, is WhileBlock -> {
                SmallControl(stringResource(R.string.mission_toggle_not), style) { actions.toggleNegated(block.id) }
                SmallControl(stringResource(R.string.mission_next_condition), style) { actions.cycleSensor(block.id) }
            }
            is CommandBlock -> Unit
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { actions.remove(block.id) }) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.mission_delete_block), tint = Color(0xFFC62F35))
        }
    }
}

@Composable
private fun SmallControl(label: String, style: BlockStyle, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 10.dp)) {
        Text(label, color = style.text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ValueChip(text: String, style: BlockStyle) {
    Text(
        text = text,
        style = BlockText,
        color = style.text,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color.White)
            .border(2.dp, style.bar, RoundedCornerShape(7.dp))
            .padding(horizontal = 6.dp),
    )
}

/** The condition inside if/while. Tapping it switches to the next sensor this mission offers. */
@Composable
private fun ConditionChip(condition: Condition, style: BlockStyle, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = condition.code,
        style = BlockText,
        color = style.text,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color.White)
            .border(2.dp, style.bar, RoundedCornerShape(7.dp))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = stringResource(R.string.mission_next_condition),
                onClick = onClick,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun Nested(style: BlockStyle, content: @Composable () -> Unit) {
    Row(Modifier.height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .padding(start = 10.dp, end = 8.dp)
                .width(3.dp)
                .fillMaxHeight()
                .background(style.bar.copy(alpha = 0.45f)),
        )
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun ClosingBrace(text: String, style: BlockStyle) {
    Text(
        text = text,
        style = BlockText,
        color = style.text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(style.tint)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** Where tapped toolbox blocks will go. Exactly one slot is the target at a time. */
@Composable
private fun SlotRow(isTarget: Boolean, onClick: () -> Unit) {
    val color = if (isTarget) Color(0xFFD98E00) else Color(0xFFB9B3D1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isTarget) Color(0xFFFFF4DA) else Color.Transparent)
            .border(2.dp, color, RoundedCornerShape(10.dp))
            .clickable(onClickLabel = stringResource(R.string.mission_add_here), onClick = onClick)
            .semantics { selected = isTarget }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(if (isTarget) R.string.mission_adding_here else R.string.mission_add_here),
            style = MaterialTheme.typography.labelMedium,
            color = if (isTarget) Color(0xFF7A4D00) else Color(0xFF7E7898),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Toolbox(mission: Mission, enabled: Boolean, onAddBlock: (PaletteBlock) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        mission.palette.forEach { item ->
            val style = styleFor(item)
            ChunkySurface(
                colors = ChunkyColors(face = style.bar, edge = style.bar.darker(), content = Color.White),
                shape = RoundedCornerShape(12.dp),
                depth = 4.dp,
                enabled = enabled,
                onClickLabel = stringResource(R.string.mission_add_block),
                onClick = { onAddBlock(item) },
                modifier = Modifier.testTag(MissionTestTags.palette(item.label)),
            ) {
                Text(
                    text = item.label,
                    style = BlockText,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private fun Color.darker(): Color = Color(red * 0.72f, green * 0.72f, blue * 0.72f, alpha)

@Composable
private fun Controls(
    uiState: MissionUiState,
    onRun: () -> Unit,
    onStep: () -> Unit,
    onBackToEditing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val space = CodeQuestTheme.colors.space
    val glass = ChunkyColors(face = space.glass, edge = Color(0x40000000), content = Color.White, border = space.glassBorder)
    val playback = uiState.playback
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (playback != null) {
            ChunkyButton(onClick = onBackToEditing, colors = glass, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text(stringResource(R.string.mission_edit))
            }
        }
        if (playback == null || (!playback.playing && !playback.finished)) {
            ChunkyButton(
                onClick = onStep,
                colors = glass,
                modifier = Modifier
                    .weight(1f)
                    .testTag(MissionTestTags.STEP),
            ) {
                Text(stringResource(R.string.mission_step))
            }
        }
        if (playback == null) {
            ChunkyButton(
                onClick = onRun,
                colors = CodeQuestTheme.colors.successButton,
                modifier = Modifier
                    .weight(1.6f)
                    .testTag(MissionTestTags.RUN),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.mission_run))
            }
        }
    }
}

@Composable
private fun ResultPanel(
    outcome: MissionOutcome,
    hasHint: Boolean,
    onBackToMap: () -> Unit,
    onEdit: () -> Unit,
    onShowHint: () -> Unit,
) {
    val success = outcome == MissionOutcome.Success
    val extra = CodeQuestTheme.colors
    val background = if (success) extra.successContainer else MaterialTheme.colorScheme.errorContainer
    val textColor = if (success) extra.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
    val reason = (outcome as? MissionOutcome.Failure)?.reason
    Surface(
        color = background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(MissionTestTags.RESULT),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(20.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ByteMascot(Modifier.size(52.dp), mood = if (success) ByteMood.CHEERING else ByteMood.WORRIED)
                Text(
                    text = if (success) stringResource(R.string.mission_success_title) else failureTitle(reason),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (success) extra.success else MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = if (success) stringResource(R.string.mission_success_body) else failureBody(reason),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
            )
            if (success) {
                ChunkyButton(onClick = onBackToMap, colors = extra.successButton, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.mission_back_to_map))
                }
                TextButton(onClick = onEdit, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.mission_keep_playing), color = textColor)
                }
            } else {
                ChunkyButton(onClick = onEdit, colors = extra.dangerButton, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.mission_try_again))
                }
                if (hasHint) {
                    TextButton(onClick = onShowHint, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.mission_show_hint), color = textColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun failureTitle(reason: FailureReason?): String = stringResource(
    when (reason) {
        FailureReason.EMPTY_PROGRAM, null -> R.string.mission_fail_empty_title
        FailureReason.BUMPED_INTO_ROCK -> R.string.mission_fail_rock_title
        FailureReason.LEFT_THE_MAP -> R.string.mission_fail_edge_title
        FailureReason.FELL_INTO_CRATER -> R.string.mission_fail_crater_title
        FailureReason.CRYSTALS_LEFT -> R.string.mission_fail_crystals_title
        FailureReason.NOT_AT_GOAL -> R.string.mission_fail_goal_title
        FailureReason.TOO_MANY_STEPS -> R.string.mission_fail_steps_title
        FailureReason.SHIELDS_GONE -> R.string.mission_fail_shields_title
    },
)

@Composable
private fun failureBody(reason: FailureReason?): String = stringResource(
    when (reason) {
        FailureReason.EMPTY_PROGRAM, null -> R.string.mission_fail_empty_body
        FailureReason.BUMPED_INTO_ROCK -> R.string.mission_fail_rock_body
        FailureReason.LEFT_THE_MAP -> R.string.mission_fail_edge_body
        FailureReason.FELL_INTO_CRATER -> R.string.mission_fail_crater_body
        FailureReason.CRYSTALS_LEFT -> R.string.mission_fail_crystals_body
        FailureReason.NOT_AT_GOAL -> R.string.mission_fail_goal_body
        FailureReason.TOO_MANY_STEPS -> R.string.mission_fail_steps_body
        FailureReason.SHIELDS_GONE -> R.string.mission_fail_shields_body
    },
)

@Composable
private fun gridDescription(state: GridState): String = stringResource(
    R.string.mission_grid_description,
    state.byte.x + 1,
    state.byte.y + 1,
    stringResource(
        when (state.heading) {
            Heading.NORTH -> R.string.heading_up
            Heading.EAST -> R.string.heading_right
            Heading.SOUTH -> R.string.heading_down
            Heading.WEST -> R.string.heading_left
        },
    ),
    state.collected,
    state.level.crystals.size,
)

@Composable
private fun bossDescription(state: BossState): String = stringResource(
    R.string.mission_boss_description,
    state.byteLane + 1,
    state.turn + 1,
    state.totalTurns,
    state.target.sorted().joinToString { (it + 1).toString() },
    state.shields,
)
