package au.edu.jcu.codequest.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.ui.components.ByteMascot
import au.edu.jcu.codequest.ui.components.ByteMood
import au.edu.jcu.codequest.ui.components.ChunkyButton
import au.edu.jcu.codequest.ui.components.LightSystemBarIcons
import au.edu.jcu.codequest.ui.components.Planet
import au.edu.jcu.codequest.ui.components.PlanetStyle
import au.edu.jcu.codequest.ui.components.SpaceBackground
import au.edu.jcu.codequest.ui.components.contentWidth
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme

object OnboardingTestTags {
    const val NEXT = "onboarding_next"
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
 * First launch, in two steps: the story that frames the course, then how the app treats the
 * learner, ending with one optional question.
 */
@Composable
fun OnboardingScreen(onChoice: (detailedHistory: Boolean) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    if (step == 0) {
        StoryStep(onNext = { step = 1 })
    } else {
        ConsentStep(onChoice = onChoice)
    }
}

/** Byte's ship has crash-landed: every lesson repairs one of its systems. */
@Composable
private fun StoryStep(onNext: () -> Unit) {
    LightSystemBarIcons()
    val space = CodeQuestTheme.colors.space
    Box(Modifier.fillMaxSize()) {
        SpaceBackground(Modifier.matchParentSize(), seed = 3)
        Planet(
            style = PlanetStyle.RINGED_VIOLET,
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopEnd)
                .padding(top = 40.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(120.dp))
            ByteMascot(Modifier.size(140.dp), mood = ByteMood.WORRIED)
            Column(
                modifier = Modifier
                    .contentWidth()
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.story_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = space.text,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.story_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = space.textSoft,
                )
                Text(
                    text = stringResource(R.string.story_goal),
                    style = MaterialTheme.typography.bodyLarge,
                    color = space.textSoft,
                )
                Spacer(Modifier.height(16.dp))
                ChunkyButton(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(OnboardingTestTags.NEXT),
                ) {
                    Text(stringResource(R.string.story_next))
                }
            }
        }
    }
}

/**
 * Says what the app stores and asks one optional question.
 *
 * The two answers are deliberately the same component with the same colours, size and weight,
 * and nothing is preselected, so neither choice is nudged (no "interface interference").
 * Declining leads to exactly the same lessons as accepting.
 */
@Composable
private fun ConsentStep(onChoice: (detailedHistory: Boolean) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.contentWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ByteMascot(Modifier.size(52.dp))
                    Text(
                        text = stringResource(R.string.onboarding_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                Text(
                    text = stringResource(R.string.onboarding_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_promise_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Promise(stringResource(R.string.onboarding_promise_offline))
                        Promise(stringResource(R.string.onboarding_promise_no_account))
                        Promise(stringResource(R.string.onboarding_promise_no_ads))
                        Promise(stringResource(R.string.onboarding_promise_no_pressure))
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
                        modifier = Modifier
                            .weight(1f)
                            .testTag(OnboardingTestTags.DECLINE),
                    )
                    ChoiceButton(
                        text = stringResource(R.string.onboarding_accept),
                        onClick = { onChoice(true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(OnboardingTestTags.ACCEPT),
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

@Composable
private fun Promise(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(CodeQuestTheme.colors.successContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = CodeQuestTheme.colors.success,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Both consent answers use this one composable so they cannot drift apart visually. */
@Composable
private fun ChoiceButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ChunkyButton(
        onClick = onClick,
        colors = CodeQuestTheme.colors.neutralButton,
        height = 56.dp,
        modifier = modifier,
    ) {
        Text(text = text, textAlign = TextAlign.Center)
    }
}
