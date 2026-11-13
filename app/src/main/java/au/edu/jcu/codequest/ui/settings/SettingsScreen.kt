@file:OptIn(ExperimentalMaterial3Api::class)

package au.edu.jcu.codequest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.model.Difficulty
import au.edu.jcu.codequest.domain.model.ThemeMode
import au.edu.jcu.codequest.domain.model.UserSettings
import au.edu.jcu.codequest.ui.components.contentWidth

object SettingsTestTags {
    const val SOUND = "settings_sound"
    const val HISTORY = "settings_history"
    const val DELETE = "settings_delete"
}

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        onSoundChange = viewModel::setSoundOn,
        onDifficultyChange = viewModel::setDifficulty,
        onThemeChange = viewModel::setThemeMode,
        onUnlockAllChange = viewModel::setUnlockAllLessons,
        onDetailedHistoryChange = viewModel::setDetailedHistory,
        onDeleteProgress = viewModel::deleteProgress,
    )
}

/** Preferences, plus the privacy controls promised on the first-run screen. */
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onSoundChange: (Boolean) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onUnlockAllChange: (Boolean) -> Unit,
    onDetailedHistoryChange: (Boolean) -> Unit,
    onDeleteProgress: () -> Unit,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.contentWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionTitle(stringResource(R.string.settings_section_learning))
                SwitchRow(
                    title = stringResource(R.string.settings_sound_title),
                    description = stringResource(R.string.settings_sound_body),
                    checked = settings.soundOn,
                    onCheckedChange = onSoundChange,
                    modifier = Modifier.testTag(SettingsTestTags.SOUND),
                )
                SwitchRow(
                    title = stringResource(R.string.settings_unlock_title),
                    description = stringResource(R.string.settings_unlock_body),
                    checked = settings.unlockAllLessons,
                    onCheckedChange = onUnlockAllChange,
                )
                ChipGroup(
                    title = stringResource(R.string.settings_difficulty_title),
                    description = stringResource(R.string.settings_difficulty_body),
                    options = Difficulty.entries,
                    selected = settings.difficulty,
                    label = { difficultyLabel(it) },
                    onSelect = onDifficultyChange,
                )
                HorizontalDivider()

                SectionTitle(stringResource(R.string.settings_section_appearance))
                ChipGroup(
                    title = stringResource(R.string.settings_theme_title),
                    description = null,
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    label = { themeLabel(it) },
                    onSelect = onThemeChange,
                )
                HorizontalDivider()

                SectionTitle(stringResource(R.string.settings_section_privacy))
                SwitchRow(
                    title = stringResource(R.string.settings_history_title),
                    description = stringResource(R.string.settings_history_body),
                    checked = settings.detailedHistory,
                    onCheckedChange = onDetailedHistoryChange,
                    modifier = Modifier.testTag(SettingsTestTags.HISTORY),
                )
                Text(
                    text = stringResource(R.string.settings_privacy_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.testTag(SettingsTestTags.DELETE),
                ) {
                    Text(stringResource(R.string.settings_delete_button))
                }
                HorizontalDivider()

                SectionTitle(stringResource(R.string.settings_section_about))
                Text(
                    text = stringResource(R.string.settings_about_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.settings_delete_dialog_title)) },
            text = { Text(stringResource(R.string.settings_delete_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteProgress()
                    },
                ) {
                    Text(stringResource(R.string.settings_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.settings_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

/**
 * A labelled switch. The whole row is the touch target and is announced as one switch,
 * which is easier to hit and clearer with TalkBack than a bare Switch.
 */
@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** A single-choice row of chips for a small set of options. */
@Composable
private fun <T> ChipGroup(
    title: String,
    description: String?,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) },
                )
            }
        }
    }
}

@Composable
private fun difficultyLabel(difficulty: Difficulty): String = stringResource(
    when (difficulty) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    },
)

@Composable
private fun themeLabel(themeMode: ThemeMode): String = stringResource(
    when (themeMode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)
