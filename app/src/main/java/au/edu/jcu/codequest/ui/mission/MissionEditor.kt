package au.edu.jcu.codequest.ui.mission

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.mission.Block
import au.edu.jcu.codequest.domain.mission.BuildTool
import au.edu.jcu.codequest.domain.mission.CommandBlock
import au.edu.jcu.codequest.domain.mission.Condition
import au.edu.jcu.codequest.domain.mission.IfBlock
import au.edu.jcu.codequest.domain.mission.Mission
import au.edu.jcu.codequest.domain.mission.PaletteBlock
import au.edu.jcu.codequest.domain.mission.RepeatBlock
import au.edu.jcu.codequest.domain.mission.Slot
import au.edu.jcu.codequest.domain.mission.WhileBlock
import au.edu.jcu.codequest.ui.components.ChunkySurface
import au.edu.jcu.codequest.ui.theme.ChunkyColors
import au.edu.jcu.codequest.ui.theme.CodeFont
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme

// The program editor of a mission: the learner's blocks, the toolbox and the free-build tools.

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

@Composable
internal fun ProgramCard(
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
internal fun Toolbox(
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

/** The tools for designing the free-build planet. The chosen one is outlined. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BuildToolbar(selected: BuildTool, onSelect: (BuildTool) -> Unit) {
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
