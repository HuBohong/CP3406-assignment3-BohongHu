package au.edu.jcu.codequest.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import androidx.hilt.navigation.compose.hiltViewModel
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.ui.components.contentWidth

object OnboardingTestTags {
    const val ACCEPT = "onboarding_accept"
    const val DECLINE = "onboarding_decline"
}

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    OnboardingScreen(onChoice = { detailedHistory -> viewModel.choose(detailedHistory, onFinished) })
}

/**
 * First-run screen: says what the app stores and asks one optional question.
 *
 * The two answers are deliberately given the same button style, size and position weight, and
 * nothing is preselected, so that neither choice is nudged (no "interface interference").
 * Declining leads to exactly the same lessons as accepting.
 */
@Composable
fun OnboardingScreen(onChoice: (detailedHistory: Boolean) -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.contentWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.onboarding_intro),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_promise_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(stringResource(R.string.onboarding_promise_offline))
                        Text(stringResource(R.string.onboarding_promise_no_account))
                        Text(stringResource(R.string.onboarding_promise_no_ads))
                        Text(stringResource(R.string.onboarding_promise_no_pressure))
                    }
                }
                Text(
                    text = stringResource(R.string.onboarding_question_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.onboarding_question_body),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ChoiceButton(
                        text = stringResource(R.string.onboarding_decline),
                        onClick = { onChoice(false) },
                        modifier = Modifier.weight(1f).testTag(OnboardingTestTags.DECLINE),
                    )
                    ChoiceButton(
                        text = stringResource(R.string.onboarding_accept),
                        onClick = { onChoice(true) },
                        modifier = Modifier.weight(1f).testTag(OnboardingTestTags.ACCEPT),
                    )
                }
                Text(
                    text = stringResource(R.string.onboarding_footnote),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Both consent answers use this one composable so they cannot drift apart visually. */
@Composable
private fun ChoiceButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 56.dp)) {
        Text(text = text, textAlign = TextAlign.Center)
    }
}
