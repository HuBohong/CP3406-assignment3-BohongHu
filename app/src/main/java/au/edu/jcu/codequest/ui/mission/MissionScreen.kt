package au.edu.jcu.codequest.ui.mission

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.mission.BossState
import au.edu.jcu.codequest.domain.mission.BuildTool
import au.edu.jcu.codequest.domain.mission.Cell
import au.edu.jcu.codequest.domain.mission.GridState
import au.edu.jcu.codequest.domain.mission.Mission
import au.edu.jcu.codequest.domain.mission.MissionKind
import au.edu.jcu.codequest.domain.mission.MissionState
import au.edu.jcu.codequest.domain.mission.PaletteBlock
import au.edu.jcu.codequest.domain.mission.Slot
import au.edu.jcu.codequest.ui.components.ByteMascot
import au.edu.jcu.codequest.ui.components.ChunkyButton
import au.edu.jcu.codequest.ui.components.CodeQuestIcons
import au.edu.jcu.codequest.ui.components.LightSystemBarIcons
import au.edu.jcu.codequest.ui.components.SpaceBackground
import au.edu.jcu.codequest.ui.components.contentWidth
import au.edu.jcu.codequest.ui.theme.ChunkyColors
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme

object MissionTestTags {
    const val RUN = "mission_run"
    const val STEP = "mission_step"
    const val RESULT = "mission_result"
    fun palette(label: String) = "mission_palette_$label"
}

/** Everything the program editor can ask the ViewModel to do. */
internal data class EditorActions(
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

/** Widest the two-pane landscape layout grows on large screens. */
private val LandscapeMaxWidth = 1100.dp

/** Free-build planet only: switching mode, choosing a tool and painting squares. */
private data class BuildActions(
    val toggle: () -> Unit,
    val selectTool: (BuildTool) -> Unit,
    val paint: (Cell) -> Unit,
)

/**
 * The end-of-unit mission: the board on top, the learner's program in the middle, the toolbox
 * underneath (in landscape the board sits on the left and the program on the right). Tapping a
 * toolbox block adds it where the highlighted "add here" slot is.
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
