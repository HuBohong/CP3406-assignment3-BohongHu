package au.edu.jcu.codequest.ui.mission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.mission.BossState
import au.edu.jcu.codequest.domain.mission.FailureReason
import au.edu.jcu.codequest.domain.mission.MissionOutcome
import au.edu.jcu.codequest.domain.mission.MissionState
import au.edu.jcu.codequest.ui.components.ByteMascot
import au.edu.jcu.codequest.ui.components.ByteMood
import au.edu.jcu.codequest.ui.components.ChunkyButton
import au.edu.jcu.codequest.ui.components.contentWidth
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme

// The sheet that slides up when a mission run ends, with its success and failure messages.

@Composable
internal fun ResultPanel(
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
