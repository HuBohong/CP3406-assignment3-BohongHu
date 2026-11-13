@file:OptIn(ExperimentalMaterial3Api::class)

package au.edu.jcu.codequest.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.ui.components.LoadingBox
import au.edu.jcu.codequest.ui.components.StarRating
import au.edu.jcu.codequest.ui.components.contentWidth

@Composable
fun HomeRoute(
    onOpenLesson: (String) -> Unit,
    onOpenBonus: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState, onOpenLesson = onOpenLesson, onOpenBonus = onOpenBonus)
}

/** Landing page: overall progress, a shortcut to the next lesson, the bonus round and the path. */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onOpenLesson: (String) -> Unit,
    onOpenBonus: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingBox(Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item(key = "progress") {
                    ProgressCard(uiState = uiState, onOpenLesson = onOpenLesson, modifier = Modifier.contentWidth())
                }
                item(key = "bonus") {
                    BonusCard(onOpenBonus = onOpenBonus, modifier = Modifier.contentWidth())
                }
                uiState.units.forEach { unit ->
                    item(key = "unit-${unit.index}") {
                        Text(
                            text = stringResource(R.string.home_unit_title, unit.index, unit.title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .contentWidth()
                                .padding(top = 12.dp)
                                .semantics { heading() },
                        )
                    }
                    items(items = unit.lessons, key = { it.id }) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            onClick = { onOpenLesson(lesson.id) },
                            modifier = Modifier.contentWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(
    uiState: HomeUiState,
    onOpenLesson: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_progress_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(
                    R.string.home_progress_summary,
                    uiState.completedLessons,
                    uiState.totalLessons,
                    uiState.totalXp,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            LinearProgressIndicator(
                progress = { uiState.pathProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.nextLesson?.let { next ->
                val labelRes = if (uiState.completedLessons == 0) {
                    R.string.home_start_lesson
                } else {
                    R.string.home_continue_lesson
                }
                Button(
                    onClick = { onOpenLesson(next.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(HomeTestTags.CONTINUE),
                ) {
                    Text(stringResource(labelRes, next.title))
                }
            }
        }
    }
}

@Composable
private fun BonusCard(onOpenBonus: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_bonus_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_bonus_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(onClick = onOpenBonus) {
                Text(stringResource(R.string.home_bonus_button))
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: LessonUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val status = stringResource(
        when {
            lesson.isLocked -> R.string.lesson_status_locked
            lesson.isCompleted -> R.string.lesson_status_completed
            else -> R.string.lesson_status_open
        },
    )
    Card(
        onClick = onClick,
        enabled = !lesson.isLocked,
        modifier = modifier.semantics { stateDescription = status },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LessonBadge(lesson)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = lesson.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (lesson.isLocked) stringResource(R.string.lesson_locked_hint) else lesson.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (lesson.isCompleted) StarRating(stars = lesson.stars)
        }
    }
}

/** Round marker at the start of a lesson row: a tick, a padlock or the lesson number. */
@Composable
private fun LessonBadge(lesson: LessonUi) {
    val colors = MaterialTheme.colorScheme
    val (background, foreground) = when {
        lesson.isLocked -> colors.surfaceVariant to colors.onSurfaceVariant
        lesson.isCompleted -> colors.primary to colors.onPrimary
        else -> colors.primaryContainer to colors.onPrimaryContainer
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = background, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            lesson.isLocked -> Icon(Icons.Filled.Lock, contentDescription = null, tint = foreground)
            lesson.isCompleted -> Icon(Icons.Filled.Check, contentDescription = null, tint = foreground)
            else -> Text(
                text = lesson.number.toString(),
                color = foreground,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

object HomeTestTags {
    const val CONTINUE = "home_continue"
}
