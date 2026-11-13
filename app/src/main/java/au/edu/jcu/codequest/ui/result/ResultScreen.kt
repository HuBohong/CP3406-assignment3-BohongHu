package au.edu.jcu.codequest.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.ui.components.StarRating
import au.edu.jcu.codequest.ui.components.contentWidth

object ResultTestTags {
    const val DONE = "result_done"
}

/**
 * Summary after a lesson. It celebrates what was achieved and then lets the learner stop:
 * there is no countdown, streak warning or "don't lose your progress" prompt.
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
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StarRating(stars = stars, starSize = 56.dp)
            Text(
                text = stringResource(if (isBonus) R.string.result_title_bonus else R.string.result_title_lesson),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.result_first_try, firstTryCorrect, total),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.result_xp, xp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = stringResource(R.string.result_no_pressure),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.contentWidth(),
            )
            Column(
                modifier = Modifier.contentWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag(ResultTestTags.DONE),
                ) {
                    Text(stringResource(R.string.result_done))
                }
                OutlinedButton(
                    onClick = onPractiseAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.result_again))
                }
            }
        }
    }
}
