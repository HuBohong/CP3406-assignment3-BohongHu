package au.edu.jcu.codequest.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.ui.components.ByteMascot
import au.edu.jcu.codequest.ui.components.ChunkyButton
import au.edu.jcu.codequest.ui.components.CodeQuestIcons
import au.edu.jcu.codequest.ui.components.LightSystemBarIcons
import au.edu.jcu.codequest.ui.components.LoadingBox
import au.edu.jcu.codequest.ui.components.Planet
import au.edu.jcu.codequest.ui.components.PlanetStyle
import au.edu.jcu.codequest.ui.components.SpaceBackground
import au.edu.jcu.codequest.ui.components.contentWidth
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme

object HomeTestTags {
    const val BONUS = "home_bonus"
    const val START = "home_popover_start"
    const val SANDBOX = "home_sandbox"
    fun node(lessonId: String) = "home_node_$lessonId"
}

/** The top bar and the bonus card come before the first unit in the map list. */
private const val ITEMS_BEFORE_UNITS = 2

@Composable
fun HomeRoute(
    onOpenLesson: (String) -> Unit,
    onOpenMission: (String) -> Unit,
    onOpenBonus: () -> Unit,
    onOpenFlight: (String) -> Unit,
    onOpenSandbox: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onOpenLesson = onOpenLesson,
        onOpenMission = onOpenMission,
        onOpenBonus = onOpenBonus,
        onOpenFlight = onOpenFlight,
        onOpenSandbox = onOpenSandbox,
    )
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
    onOpenFlight: (String) -> Unit = {},
    onOpenSandbox: () -> Unit = {},
) {
    LightSystemBarIcons()
    var openLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentLessonId = uiState.currentNodeId
    val listState = rememberLazyListState()

    // Opens the map at the learner's current unit, once; after that the map stays where they left it.
    var openedAtCurrentUnit by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading || openedAtCurrentUnit) return@LaunchedEffect
        val unitPosition = uiState.units.indexOfFirst { unit ->
            unit.lessons.any { it.id == currentLessonId } || unit.mission?.id == currentLessonId
        }
        if (unitPosition > 0) listState.scrollToItem(ITEMS_BEFORE_UNITS + unitPosition)
        openedAtCurrentUnit = true
    }

    Box(Modifier.fillMaxSize()) {
        SpaceBackground(Modifier.matchParentSize())
        if (uiState.isLoading) {
            LoadingBox(color = Color.White)
        } else {
            val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            LazyColumn(
                state = listState,
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
                        onStartFlight = { id ->
                            openLessonId = null
                            onOpenFlight(id)
                        },
                        modifier = Modifier.contentWidth().padding(top = 24.dp),
                    )
                }
                item(key = "sandbox") {
                    SandboxCard(
                        collected = uiState.blocksCollected,
                        total = uiState.blocksTotal,
                        unlocked = uiState.sandboxUnlocked,
                        onOpen = onOpenSandbox,
                        modifier = Modifier.contentWidth().padding(top = 8.dp),
                    )
                }
            }
            // Keeps the status bar readable when the map scrolls underneath it.
            val scrim = CodeQuestTheme.colors.space.top
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topInset + 8.dp)
                    .background(Brush.verticalGradient(0f to scrim, 0.7f to scrim.copy(alpha = 0.9f), 1f to Color.Transparent)),
            )
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
        val playDescription = stringResource(R.string.home_bonus_button_description)
        ChunkyButton(
            onClick = onOpenBonus,
            colors = CodeQuestTheme.colors.xpButton,
            height = 42.dp,
            contentPadding = PaddingValues(horizontal = 18.dp),
            modifier = Modifier
                .testTag(HomeTestTags.BONUS)
                // The button only says "Play"; TalkBack hears what it plays.
                .semantics { contentDescription = playDescription },
        ) {
            Text(stringResource(R.string.home_bonus_button), modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

/**
 * The free-build planet at the end of the map: once Byte has move(), the learner can design a
 * planet and program it with every block collected so far.
 */
@Composable
private fun SandboxCard(collected: Int, total: Int, unlocked: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val space = CodeQuestTheme.colors.space
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(space.glass)
            .border(2.dp, space.glassBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Planet(style = PlanetStyle.forUnit(5), modifier = Modifier.size(52.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_sandbox_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = space.text,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.home_sandbox_blocks, collected, total),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFFC94D),
                )
            }
        }
        Text(
            text = stringResource(if (unlocked) R.string.home_sandbox_body else R.string.home_sandbox_locked),
            style = MaterialTheme.typography.bodyMedium,
            color = space.textSoft,
        )
        if (unlocked) {
            ChunkyButton(
                onClick = onOpen,
                height = 48.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HomeTestTags.SANDBOX),
            ) {
                Text(stringResource(R.string.home_sandbox_open))
            }
        }
    }
}
