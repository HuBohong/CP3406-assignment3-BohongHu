package au.edu.jcu.codequest.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.ui.components.ByteMascot
import au.edu.jcu.codequest.ui.components.ChunkyButton
import au.edu.jcu.codequest.ui.components.ChunkySurface
import au.edu.jcu.codequest.ui.components.CodeQuestIcons
import au.edu.jcu.codequest.ui.components.LightSystemBarIcons
import au.edu.jcu.codequest.ui.components.LoadingBox
import au.edu.jcu.codequest.ui.components.Planet
import au.edu.jcu.codequest.ui.components.PlanetStyle
import au.edu.jcu.codequest.ui.components.SpaceBackground
import au.edu.jcu.codequest.ui.components.StarRating
import au.edu.jcu.codequest.ui.components.contentWidth
import au.edu.jcu.codequest.ui.components.rememberReducedMotion
import au.edu.jcu.codequest.ui.theme.ChunkyColors
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme
import au.edu.jcu.codequest.ui.theme.UnitColors

object HomeTestTags {
    const val BONUS = "home_bonus"
    const val START = "home_popover_start"
    fun node(lessonId: String) = "home_node_$lessonId"
}

private val NodeSize = 72.dp
private val CurrentNodeSize = 82.dp
private val NodeDepth = 7.dp
private val NodeStep = 116.dp
private val PathTopPadding = 56.dp
private val PathBottomPadding = 44.dp

/** Sideways position of each lesson along the winding path, repeating every four lessons. */
private val NodeOffsets = listOf(0.dp, (-72).dp, 0.dp, 72.dp)

private fun nodeOffset(lessonNumber: Int): Dp = NodeOffsets[(lessonNumber - 1).mod(NodeOffsets.size)]

@Composable
fun HomeRoute(
    onOpenLesson: (String) -> Unit,
    onOpenMission: (String) -> Unit,
    onOpenBonus: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState, onOpenLesson = onOpenLesson, onOpenMission = onOpenMission, onOpenBonus = onOpenBonus)
}

/**
 * Landing page: a map of the course as a journey through space. Each unit is a planet,
 * each lesson a stop on a winding path. Tapping a stop shows what it is about and starts it.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onOpenLesson: (String) -> Unit,
    onOpenMission: (String) -> Unit,
    onOpenBonus: () -> Unit,
) {
    LightSystemBarIcons()
    var openLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentLessonId = uiState.currentNodeId

    Box(Modifier.fillMaxSize()) {
        SpaceBackground(Modifier.matchParentSize())
        if (uiState.isLoading) {
            LoadingBox(color = Color.White)
        } else {
            val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topInset + 8.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item(key = "top") {
                    HomeTopBar(uiState = uiState, modifier = Modifier.contentWidth())
                }
                item(key = "bonus") {
                    BonusCard(onOpenBonus = onOpenBonus, modifier = Modifier.contentWidth().padding(top = 12.dp))
                }
                items(items = uiState.units, key = { "unit-${it.index}" }) { unit ->
                    UnitSection(
                        unit = unit,
                        colors = CodeQuestTheme.colors.units[(unit.index - 1).mod(CodeQuestTheme.colors.units.size)],
                        currentLessonId = currentLessonId,
                        openLessonId = openLessonId,
                        onNodeClick = { id -> openLessonId = if (openLessonId == id) null else id },
                        onDismissPopover = { openLessonId = null },
                        onStartLesson = { id ->
                            openLessonId = null
                            onOpenLesson(id)
                        },
                        onStartMission = { id ->
                            openLessonId = null
                            onOpenMission(id)
                        },
                        modifier = Modifier.contentWidth().padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(uiState: HomeUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ByteMascot(Modifier.size(38.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        StatChip(
            icon = CodeQuestIcons.Book,
            text = stringResource(R.string.home_lessons_chip, uiState.completedLessons, uiState.totalLessons),
            description = stringResource(
                R.string.home_lessons_chip_description,
                uiState.completedLessons,
                uiState.totalLessons,
            ),
            tint = Color(0xFFD9CCFF),
        )
        StatChip(
            icon = CodeQuestIcons.Bolt,
            text = uiState.totalXp.toString(),
            description = stringResource(R.string.home_xp_chip_description, uiState.totalXp),
            tint = Color(0xFFFFC94D),
        )
    }
}

/** A glassy pill with an icon and a number. TalkBack reads [description] instead of the parts. */
@Composable
private fun StatChip(icon: ImageVector, text: String, description: String, tint: Color) {
    val space = CodeQuestTheme.colors.space
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(17.dp))
            .background(space.glass)
            .border(2.dp, space.glassBorder, RoundedCornerShape(17.dp))
            .height(34.dp)
            .padding(horizontal = 10.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

@Composable
private fun BonusCard(onOpenBonus: () -> Unit, modifier: Modifier = Modifier) {
    val space = CodeQuestTheme.colors.space
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(space.glass)
            .border(2.dp, space.glassBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x2EFFB31A)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(CodeQuestIcons.Gift, contentDescription = null, tint = Color(0xFFFFC94D), modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_bonus_title),
                style = MaterialTheme.typography.titleMedium,
                color = space.text,
            )
            Text(
                text = stringResource(R.string.home_bonus_body),
                style = MaterialTheme.typography.bodyMedium,
                color = space.textSoft,
            )
        }
        ChunkyButton(
            onClick = onOpenBonus,
            colors = CodeQuestTheme.colors.xpButton,
            height = 42.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.testTag(HomeTestTags.BONUS),
        ) {
            Text(stringResource(R.string.home_bonus_button))
        }
    }
}

/** One unit: its banner, its planet and its lessons as stops on a dotted flight path. */
@Composable
private fun UnitSection(
    unit: UnitUi,
    colors: UnitColors,
    currentLessonId: String?,
    openLessonId: String?,
    onNodeClick: (String) -> Unit,
    onDismissPopover: () -> Unit,
    onStartLesson: (String) -> Unit,
    onStartMission: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        UnitBanner(unit = unit, colors = colors)

        val lessons = unit.lessons
        val mission = unit.mission
        // Sideways offset of every stop: the lessons, then the mission that closes the unit.
        val offsets = lessons.map { nodeOffset(it.number) } +
            listOfNotNull(mission?.let { nodeOffset((lessons.lastOrNull()?.number ?: 0) + 1) })
        val areaHeight = PathTopPadding + NodeStep * (offsets.size - 1).coerceAtLeast(0) +
            CurrentNodeSize + NodeDepth + PathBottomPadding
        val planetOnLeft = offsets.none { it < 0.dp }
        val density = LocalDensity.current
        val trail = CodeQuestTheme.colors.space.trail

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(areaHeight),
        ) {
            Planet(
                style = PlanetStyle.forUnit(unit.index),
                modifier = Modifier
                    .size(112.dp)
                    .align(if (planetOnLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .offset(x = if (planetOnLeft) (-16).dp else 16.dp),
            )

            // Dotted flight path through the centre of every node.
            Canvas(Modifier.matchParentSize()) {
                if (offsets.size < 2) return@Canvas
                val centres = offsets.mapIndexed { i, offset ->
                    with(density) {
                        Offset(
                            x = size.width / 2f + offset.toPx(),
                            y = (PathTopPadding + NodeStep * i + NodeSize / 2).toPx(),
                        )
                    }
                }
                val path = Path().apply {
                    moveTo(centres.first().x, centres.first().y)
                    centres.zipWithNext { a, b ->
                        val midY = (a.y + b.y) / 2f
                        cubicTo(a.x, midY, b.x, midY, b.x, b.y)
                    }
                }
                val gap = with(density) { 18.dp.toPx() }
                drawPath(
                    path = path,
                    color = trail,
                    style = Stroke(
                        width = with(density) { 8.dp.toPx() },
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, gap)),
                    ),
                )
            }

            lessons.forEachIndexed { i, lesson ->
                LessonNode(
                    lesson = lesson,
                    colors = colors,
                    isCurrent = lesson.id == currentLessonId,
                    isPopoverOpen = lesson.id == openLessonId,
                    onClick = { onNodeClick(lesson.id) },
                    onDismissPopover = onDismissPopover,
                    onStart = { onStartLesson(lesson.id) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = nodeOffset(lesson.number), y = PathTopPadding + NodeStep * i),
                )
            }
            if (mission != null) {
                MissionNode(
                    mission = mission,
                    isCurrent = mission.id == currentLessonId,
                    isPopoverOpen = mission.id == openLessonId,
                    onClick = { onNodeClick(mission.id) },
                    onDismissPopover = onDismissPopover,
                    onStart = { onStartMission(mission.id) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = offsets.last(), y = PathTopPadding + NodeStep * lessons.size),
                )
            }
        }
    }
}

/** The stop that closes a unit: a mission (amber flag) or a boss fight (red target). */
@Composable
private fun MissionNode(
    mission: MissionNodeUi,
    isCurrent: Boolean,
    isPopoverOpen: Boolean,
    onClick: () -> Unit,
    onDismissPopover: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val space = CodeQuestTheme.colors.space
    val extra = CodeQuestTheme.colors
    val status = stringResource(
        when {
            mission.isLocked -> R.string.lesson_status_locked
            mission.isCompleted -> R.string.lesson_status_completed
            else -> R.string.lesson_status_open
        },
    )
    val kind = stringResource(if (mission.isBoss) R.string.home_boss_kind else R.string.home_mission_kind)
    val description = stringResource(R.string.home_mission_node_description, kind, mission.title, status)
    val colors = when {
        mission.isLocked -> ChunkyColors(face = space.lockFill, edge = space.lockEdge, content = space.lockInk)
        mission.isBoss -> extra.dangerButton
        else -> extra.xpButton
    }
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        if (isCurrent) {
            PulseRing(color = if (mission.isBoss) Color(0xFFFF8A8E) else Color(0xFFFFC94D), nodeSize = CurrentNodeSize)
            StartBubble(color = colors.face, modifier = Modifier.offset(y = (-48).dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ChunkySurface(
                colors = colors,
                shape = CircleShape,
                depth = NodeDepth,
                onClick = onClick,
                onClickLabel = stringResource(R.string.home_node_click_label),
                modifier = Modifier
                    .width(CurrentNodeSize)
                    .height(CurrentNodeSize + NodeDepth)
                    .semantics { contentDescription = description }
                    .testTag(HomeTestTags.node(mission.id)),
            ) {
                Icon(
                    imageVector = when {
                        mission.isLocked -> Icons.Filled.Lock
                        mission.isCompleted -> Icons.Filled.Check
                        mission.isBoss -> CodeQuestIcons.Target
                        else -> CodeQuestIcons.Flag
                    },
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                )
            }
            Text(
                text = mission.title,
                style = MaterialTheme.typography.labelLarge,
                color = if (mission.isLocked) space.lockInk else Color.White,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clearAndSetSemantics {},
            )
        }
        if (isPopoverOpen) {
            val belowNode = with(LocalDensity.current) { (CurrentNodeSize + NodeDepth + 34.dp).roundToPx() }
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, belowNode),
                onDismissRequest = onDismissPopover,
                properties = PopupProperties(focusable = true),
            ) {
                MissionPopover(mission = mission, kind = kind, colors = colors, onStart = onStart)
            }
        }
    }
}

@Composable
private fun MissionPopover(mission: MissionNodeUi, kind: String, colors: ChunkyColors, onStart: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 8.dp,
        modifier = Modifier.width(288.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = kind, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(text = mission.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (mission.isLocked) stringResource(R.string.home_mission_locked) else mission.goal,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!mission.isLocked) {
                Spacer(Modifier.height(4.dp))
                ChunkyButton(
                    onClick = onStart,
                    colors = colors,
                    height = 48.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(HomeTestTags.START),
                ) {
                    Text(
                        stringResource(if (mission.isCompleted) R.string.home_mission_again else R.string.home_mission_start),
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitBanner(unit: UnitUi, colors: UnitColors) {
    val done = unit.lessons.count { it.isCompleted }
    val countDescription = stringResource(R.string.home_unit_count_description, done, unit.lessons.size)
    ChunkySurface(
        colors = ChunkyColors(face = colors.fill, edge = colors.edge, content = Color.White),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { heading() },
            ) {
                Text(
                    text = stringResource(R.string.home_unit_label, unit.index),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.9f),
                )
                Text(text = unit.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Text(
                text = stringResource(R.string.home_unit_count, done, unit.lessons.size),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .clearAndSetSemantics { contentDescription = countDescription },
            )
        }
    }
}

@Composable
private fun LessonNode(
    lesson: LessonUi,
    colors: UnitColors,
    isCurrent: Boolean,
    isPopoverOpen: Boolean,
    onClick: () -> Unit,
    onDismissPopover: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val space = CodeQuestTheme.colors.space
    val size = if (isCurrent) CurrentNodeSize else NodeSize
    val status = stringResource(
        when {
            lesson.isLocked -> R.string.lesson_status_locked
            lesson.isCompleted -> R.string.lesson_status_completed
            else -> R.string.lesson_status_open
        },
    )
    val description = stringResource(R.string.home_node_description, lesson.number, lesson.title, status)
    val nodeColors = if (lesson.isLocked) {
        ChunkyColors(face = space.lockFill, edge = space.lockEdge, content = space.lockInk)
    } else {
        ChunkyColors(face = colors.fill, edge = colors.edge, content = Color.White)
    }

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        if (isCurrent) {
            PulseRing(color = Color(0xFF9CC8FF), nodeSize = size)
            StartBubble(
                color = colors.fill,
                modifier = Modifier.offset(y = (-48).dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ChunkySurface(
                colors = nodeColors,
                shape = CircleShape,
                depth = NodeDepth,
                onClick = onClick,
                onClickLabel = stringResource(R.string.home_node_click_label),
                modifier = Modifier
                    .width(size)
                    .height(size + NodeDepth)
                    .semantics { contentDescription = description }
                    .testTag(HomeTestTags.node(lesson.id)),
            ) {
                val icon = when {
                    lesson.isLocked -> Icons.Filled.Lock
                    lesson.isCompleted -> Icons.Filled.Check
                    isCurrent -> Icons.Filled.PlayArrow
                    else -> null
                }
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(size * 0.45f))
                } else {
                    Text(text = lesson.number.toString(), style = MaterialTheme.typography.headlineSmall)
                }
            }
            if (lesson.isCompleted) {
                StarRating(
                    stars = lesson.stars,
                    starSize = 15.dp,
                    emptyColor = space.lockFill,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        if (isPopoverOpen) {
            val belowNode = with(LocalDensity.current) { (size + NodeDepth + 30.dp).roundToPx() }
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, belowNode),
                onDismissRequest = onDismissPopover,
                properties = PopupProperties(focusable = true),
            ) {
                LessonPopover(lesson = lesson, colors = colors, onStart = onStart)
            }
        }
    }
}

/** A soft ring that breathes around the next lesson, inviting a tap. Still when motion is reduced. */
@Composable
private fun PulseRing(color: Color, nodeSize: Dp) {
    val reducedMotion = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "pulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "pulsePhase",
    )
    Box(
        Modifier
            .offset(y = (-15).dp)
            .size(nodeSize + 30.dp)
            .drawBehind {
                val base = size.minDimension / 2f - 4.dp.toPx()
                drawCircle(
                    color = color.copy(alpha = 0.35f * (1f - phase)),
                    radius = base + phase * 10.dp.toPx(),
                    style = Stroke(width = 5.dp.toPx()),
                )
                drawCircle(color = color.copy(alpha = 0.25f), radius = base, style = Stroke(width = 5.dp.toPx()))
            },
    )
}

@Composable
private fun StartBubble(color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Text(
            text = stringResource(R.string.home_start_bubble),
            style = MaterialTheme.typography.titleSmall,
            color = color,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clearAndSetSemantics {},
        )
        // Little pointer under the bubble.
        Canvas(
            Modifier
                .padding(top = 33.dp)
                .size(width = 16.dp, height = 8.dp),
        ) {
            drawPath(
                Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                },
                Color.White,
            )
        }
    }
}

/** Card that opens under a lesson stop: what the lesson is about and a button to start it. */
@Composable
private fun LessonPopover(lesson: LessonUi, colors: UnitColors, onStart: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 8.dp,
        modifier = Modifier.width(288.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_popover_lesson, lesson.number),
                style = MaterialTheme.typography.labelLarge,
                color = colors.fill,
            )
            Text(text = lesson.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (lesson.isLocked) stringResource(R.string.lesson_locked_hint) else lesson.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
            if (lesson.isCompleted) StarRating(stars = lesson.stars, starSize = 18.dp)
            if (!lesson.isLocked) {
                Spacer(Modifier.height(4.dp))
                ChunkyButton(
                    onClick = onStart,
                    colors = ChunkyColors(face = colors.fill, edge = colors.edge, content = Color.White),
                    height = 48.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(HomeTestTags.START),
                ) {
                    Text(
                        stringResource(
                            if (lesson.isCompleted) R.string.home_popover_again else R.string.home_popover_start,
                        ),
                    )
                }
            }
        }
    }
}
