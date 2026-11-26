package au.edu.jcu.codequest.ui.mission

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import au.edu.jcu.codequest.domain.mission.BuildTool
import au.edu.jcu.codequest.domain.mission.Cell
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
import au.edu.jcu.codequest.domain.mission.Tile
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
        buildActions = BuildActions(
            toggle = viewModel::toggleBuildMode,
            selectTool = viewModel::selectTool,
            paint = viewModel::paint,
        ),
    )
}

/** Free-build planet only: switching mode, choosing a tool and painting squares. */
/** Widest the two-pane landscape layout grows on large screens. */
private val LandscapeMaxWidth = 1100.dp

private data class BuildActions(
    val toggle: () -> Unit,
    val selectTool: (BuildTool) -> Unit,
    val paint: (Cell) -> Unit,
)

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
    buildActions: BuildActions,
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
        val configuration = LocalConfiguration.current
        // In landscape the board sits beside the program instead of above it.
        val landscape = configuration.screenWidthDp > configuration.screenHeightDp
        // Portrait: the board takes about a third of the height, leaving room for the program.
        val boardMax = if (landscape) {
            (configuration.screenHeightDp.dp * 0.6f).coerceIn(150.dp, 360.dp)
        } else {
            (configuration.screenHeightDp.dp * 0.36f).coerceIn(150.dp, 300.dp)
        }
        val boardAndGoal: @Composable () -> Unit = {
            when (val board = uiState.boardState) {
                is GridState -> GridBoard(
                    board,
                    description = gridDescription(board),
                    maxBoardHeight = boardMax,
                    onCellTap = if (uiState.buildMode && uiState.isEditable) buildActions.paint else null,
                    cellLabels = if (uiState.buildMode) sandboxCellLabels(board) else null,
                    cellActionLabel = stringResource(R.string.sandbox_paint_cell),
                )
                is BossState -> BossArena(
                    board,
                    description = bossDescription(board),
                    maxBoardHeight = minOf(boardMax, 260.dp),
                )
                null -> Unit
            }
            GoalLine(mission = mission, showHint = uiState.showHint, onToggleHint = onToggleHint)
        }
        val programAndTools: @Composable ColumnScope.() -> Unit = {
            ProgramCard(
                uiState = uiState,
                actions = editorActions,
                onClear = onClear,
                modifier = Modifier.weight(1f),
            )
            if (uiState.buildMode) {
                BuildToolbar(selected = uiState.buildTool, onSelect = buildActions.selectTool)
            } else {
                Toolbox(
                    mission = mission,
                    enabled = uiState.isEditable,
                    isCollected = uiState::isCollected,
                    onAddBlock = onAddBlock,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MissionTopBar(
                mission = mission,
                state = uiState.boardState,
                onExit = onExit,
                buildMode = if (uiState.isSandbox) uiState.buildMode else null,
                buildEnabled = uiState.isEditable,
                onToggleBuild = buildActions.toggle,
                modifier = if (landscape) Modifier.widthIn(max = LandscapeMaxWidth).fillMaxWidth() else Modifier.contentWidth(),
            )
            if (landscape) {
                Row(
                    modifier = Modifier
                        .widthIn(max = LandscapeMaxWidth)
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        boardAndGoal()
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        programAndTools()
                        Controls(
                            uiState = uiState,
                            onRun = onRun,
                            onStep = onStep,
                            onBackToEditing = onBackToEditing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .contentWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    boardAndGoal()
                    programAndTools()
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
        }
        AnimatedVisibility(
            visible = uiState.outcome != null,
            enter = slideInVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            uiState.outcome?.let { outcome ->
                ResultPanel(
                    outcome = outcome,
                    boardState = uiState.boardState,
                    isSandbox = uiState.isSandbox,
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
private fun MissionTopBar(
    mission: Mission,
    state: MissionState?,
    onExit: () -> Unit,
    /** Null for the built-in missions; on the free-build planet, whether Build is on. */
    buildMode: Boolean? = null,
    buildEnabled: Boolean = true,
    onToggleBuild: () -> Unit = {},
    modifier: Modifier = Modifier.contentWidth(),
) {
    val space = CodeQuestTheme.colors.space
    Row(
        modifier = modifier
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
                text = when {
                    buildMode != null -> stringResource(R.string.sandbox_label)
                    else -> stringResource(
                        if (mission.kind == MissionKind.BOSS) R.string.mission_boss_label else R.string.mission_unit_label,
                        mission.unitIndex,
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                color = space.textSoft,
            )
            Text(text = mission.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
        if (buildMode != null) {
            BuildToggle(buildMode = buildMode, enabled = buildEnabled, onToggle = onToggleBuild)
        }
        if (state is GridState && state.level.crystals.isNotEmpty()) {
            StatusChip(
                icon = CodeQuestIcons.Gem,
                text = stringResource(R.string.mission_count, state.collected, state.level.crystals.size),
                description = stringResource(R.string.mission_crystals_description, state.collected, state.level.crystals.size),
                color = Color(0xFF8FF5E4),
            )
        }
        if (state is BossState) {
            StatusChip(
                icon = CodeQuestIcons.Shield,
                text = stringResource(R.string.mission_count, state.shields, state.level.shields),
                description = stringResource(R.string.mission_shields_description, state.shields, state.level.shields),
                color = Color(0xFFFFC94D),
            )
        }
    }
}

@Composable
private fun StatusChip(icon: ImageVector, text: String, description: String, color: Color) {
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
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
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
            SlotRow(isTarget = ui.target == slot, revision = ui.program, onClick = { actions.selectTarget(slot) })
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
 * replay gets an amber outline, matching the highlighted line in lesson code, and is scrolled
 * into view so long programs can be followed while they run.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(running) {
        if (running) bringIntoView.bringIntoView()
    }
    Column(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoView)
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

/**
 * Where tapped toolbox blocks will go. Exactly one slot is the target at a time, and the
 * program list scrolls to keep it in view as blocks are added ([revision] changes with every edit).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlotRow(isTarget: Boolean, revision: Any?, onClick: () -> Unit) {
    val color = if (isTarget) Color(0xFFD98E00) else Color(0xFFB9B3D1)
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(isTarget, revision) {
        if (isTarget) bringIntoView.bringIntoView()
    }
    Row(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoView)
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
private fun Toolbox(
    mission: Mission,
    enabled: Boolean,
    isCollected: (PaletteBlock) -> Boolean,
    onAddBlock: (PaletteBlock) -> Unit,
) {
    val space = CodeQuestTheme.colors.space
    val anyLocked = mission.palette.any { !isCollected(it) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            mission.palette.forEach { item ->
                val collected = isCollected(item)
                val style = styleFor(item)
                val lockedDescription = stringResource(R.string.mission_block_locked_description, item.label)
                ChunkySurface(
                    colors = if (collected) {
                        ChunkyColors(face = style.bar, edge = style.bar.darker(), content = Color.White)
                    } else {
                        ChunkyColors(face = space.lockFill, edge = space.lockEdge, content = space.lockInk)
                    },
                    shape = RoundedCornerShape(12.dp),
                    depth = 4.dp,
                    enabled = enabled && collected,
                    onClickLabel = stringResource(R.string.mission_add_block),
                    onClick = { onAddBlock(item) },
                    modifier = Modifier
                        .testTag(MissionTestTags.palette(item.label))
                        .then(if (collected) Modifier else Modifier.semantics { contentDescription = lockedDescription }),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (!collected) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = space.lockInk, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = item.label,
                            style = BlockText,
                            color = when {
                                !collected -> space.lockInk
                                enabled -> Color.White
                                else -> Color.White.copy(alpha = 0.5f)
                            },
                        )
                    }
                }
            }
        }
        if (anyLocked) {
            Text(
                text = stringResource(R.string.mission_blocks_locked_note),
                style = MaterialTheme.typography.bodySmall,
                color = space.textSoft,
            )
        }
    }
}

/** Free-build planet: a toggle between designing the planet and writing the program. */
@Composable
private fun BuildToggle(buildMode: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val space = CodeQuestTheme.colors.space
    TextButton(
        onClick = onToggle,
        enabled = enabled,
        modifier = Modifier.padding(end = 6.dp),
    ) {
        Text(
            text = stringResource(if (buildMode) R.string.sandbox_done_building else R.string.sandbox_build),
            color = if (enabled) Color(0xFFFFC94D) else space.textSoft,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** The tools for designing the free-build planet. The chosen one is outlined. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuildToolbar(selected: BuildTool, onSelect: (BuildTool) -> Unit) {
    val space = CodeQuestTheme.colors.space
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.sandbox_build_help),
            style = MaterialTheme.typography.bodySmall,
            color = space.textSoft,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BuildTool.entries.forEach { tool ->
                val isSelected = tool == selected
                ChunkySurface(
                    colors = ChunkyColors(
                        face = if (isSelected) Color(0xFF5A32D6) else space.glass,
                        edge = Color(0x40000000),
                        content = Color.White,
                        border = if (isSelected) Color(0xFFFFC94D) else space.glassBorder,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    depth = 4.dp,
                    borderWidth = 2.dp,
                    onClickLabel = stringResource(R.string.sandbox_choose_tool),
                    onClick = { onSelect(tool) },
                    modifier = Modifier.semantics { this.selected = isSelected },
                ) {
                    Text(
                        text = stringResource(toolLabel(tool)),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private fun toolLabel(tool: BuildTool): Int = when (tool) {
    BuildTool.FLOOR -> R.string.sandbox_tool_floor
    BuildTool.CRYSTAL -> R.string.sandbox_tool_crystal
    BuildTool.ROCK -> R.string.sandbox_tool_rock
    BuildTool.CRATER -> R.string.sandbox_tool_crater
    BuildTool.GOAL -> R.string.sandbox_tool_goal
    BuildTool.START -> R.string.sandbox_tool_start
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
    boardState: MissionState?,
    isSandbox: Boolean,
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
        // A centred sheet on wide screens and in landscape instead of a full-width strip.
        modifier = Modifier
            .contentWidth()
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
                text = when {
                    !success -> failureBody(reason)
                    isSandbox -> stringResource(R.string.sandbox_success_body)
                    else -> successBody(boardState)
                },
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

/**
 * After a boss fight, says how many shields Byte kept: a perfect run is celebrated, and a run
 * that took hits invites the learner to improve the program (the mission is already passed).
 */
@Composable
private fun successBody(boardState: MissionState?): String {
    val boss = boardState as? BossState ?: return stringResource(R.string.mission_success_body)
    return if (boss.shields == boss.level.shields) {
        stringResource(R.string.mission_success_boss_perfect)
    } else {
        stringResource(R.string.mission_success_boss_hit, boss.shields, boss.level.shields)
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

/** "Row 2, column 3: rock" for every square of the free-build planet, row by row. */
@Composable
private fun sandboxCellLabels(state: GridState): List<String> {
    val context = LocalContext.current
    val names = mapOf(
        "byte" to stringResource(R.string.sandbox_tool_start),
        "crystal" to stringResource(R.string.sandbox_tool_crystal),
        "rock" to stringResource(R.string.sandbox_tool_rock),
        "crater" to stringResource(R.string.sandbox_tool_crater),
        "goal" to stringResource(R.string.sandbox_tool_goal),
        "floor" to stringResource(R.string.sandbox_tool_floor),
    )
    val level = state.level
    return (0 until level.height).flatMap { row ->
        (0 until level.width).map { column ->
            val cell = Cell(column, row)
            val what = when {
                cell == state.byte -> names.getValue("byte")
                cell in state.crystalsLeft -> names.getValue("crystal")
                else -> when (level.tileAt(cell)) {
                    Tile.ROCK -> names.getValue("rock")
                    Tile.CRATER -> names.getValue("crater")
                    Tile.GOAL -> names.getValue("goal")
                    Tile.FLOOR, null -> names.getValue("floor")
                }
            }
            context.getString(R.string.sandbox_cell, row + 1, column + 1, what)
        }
    }
}

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
