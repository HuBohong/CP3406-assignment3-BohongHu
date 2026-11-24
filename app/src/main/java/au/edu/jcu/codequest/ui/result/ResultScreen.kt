package au.edu.jcu.codequest.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.quiz.Scoring
import au.edu.jcu.codequest.ui.components.ByteMascot
import au.edu.jcu.codequest.ui.components.ByteMood
import au.edu.jcu.codequest.ui.components.ChunkyButton
import au.edu.jcu.codequest.ui.components.CodeQuestIcons
import au.edu.jcu.codequest.ui.components.LightSystemBarIcons
import au.edu.jcu.codequest.ui.components.SpaceBackground
import au.edu.jcu.codequest.ui.components.contentWidth
import au.edu.jcu.codequest.ui.theme.ChunkyColors
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme

object ResultTestTags {
    const val DONE = "result_done"
}

/**
 * "Mission complete" after a lesson, on the same space background as the map. It celebrates
 * what was achieved and then lets the learner stop: there is no countdown, streak warning or
 * "don't lose your progress" prompt.
 */
@Composable
fun ResultScreen(
    stars: Int,
    xp: Int,
    firstTryCorrect: Int,
    total: Int,
    isBonus: Boolean,
    onDone: () -> Unit,
    onPractiseAgain: () -> Unit,
) {
    BackHandler(onBack = onDone)
    LightSystemBarIcons()
    val space = CodeQuestTheme.colors.space

    Box(Modifier.fillMaxSize()) {
        SpaceBackground(Modifier.matchParentSize(), seed = 11)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(32.dp))
                ByteMascot(Modifier.size(128.dp), mood = ByteMood.CHEERING)
                Text(
                    text = stringResource(if (isBonus) R.string.result_title_bonus else R.string.result_title_lesson),
                    style = MaterialTheme.typography.headlineMedium,
                    color = space.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .semantics { heading() },
                )
                BigStars(stars = stars, modifier = Modifier.padding(vertical = 20.dp))
                Row(
                    modifier = Modifier.contentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ResultTile(
                        label = stringResource(R.string.result_xp_label),
                        value = stringResource(R.string.result_xp, xp),
                        accent = Color(0xFFFFC94D),
                        showBolt = true,
                        modifier = Modifier.weight(1f),
                    )
                    ResultTile(
                        label = stringResource(R.string.result_first_try_label),
                        value = stringResource(R.string.result_first_try_value, firstTryCorrect, total),
                        accent = Color(0xFF7EE0A8),
                        showBolt = false,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(R.string.result_no_pressure),
                    style = MaterialTheme.typography.bodyMedium,
                    color = space.textSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .contentWidth()
                        .padding(top = 20.dp, bottom = 16.dp),
                )
            }
            Column(
                modifier = Modifier
                    .contentWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChunkyButton(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ResultTestTags.DONE),
                ) {
                    Text(stringResource(R.string.result_done))
                }
                ChunkyButton(
                    onClick = onPractiseAgain,
                    colors = ChunkyColors(
                        face = space.glass,
                        edge = Color(0x33000000),
                        content = Color.White,
                        border = space.glassBorder,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.result_again))
                }
            }
        }
    }
}

/** Three large stars, the outer two tilted. Read as one phrase, e.g. "2 of 3 stars". */
@Composable
private fun BigStars(stars: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.stars_description, stars, Scoring.MAX_STARS)
    val empty = CodeQuestTheme.colors.space.lockFill
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val sizes = listOf(56.dp, 72.dp, 56.dp)
        val tilt = listOf(-12f, 0f, 12f)
        repeat(Scoring.MAX_STARS) { index ->
            StarIcon(
                filled = index < stars,
                size = sizes[index],
                emptyColor = empty,
                modifier = Modifier.rotate(tilt[index]),
            )
        }
    }
}

@Composable
private fun StarIcon(filled: Boolean, size: Dp, emptyColor: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = null,
        tint = if (filled) CodeQuestTheme.colors.xp else emptyColor,
        modifier = modifier.size(size),
    )
}

@Composable
private fun ResultTile(
    label: String,
    value: String,
    accent: Color,
    showBolt: Boolean,
    modifier: Modifier = Modifier,
) {
    val description = "$label: $value"
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CodeQuestTheme.colors.space.glass)
            .border(2.dp, accent, RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 14.dp)
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = accent, textAlign = TextAlign.Center)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showBolt) {
                Icon(CodeQuestIcons.Bolt, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        }
    }
}
