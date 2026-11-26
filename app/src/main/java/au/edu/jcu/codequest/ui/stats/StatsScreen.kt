@file:OptIn(ExperimentalMaterial3Api::class)

package au.edu.jcu.codequest.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.model.RecentSession
import au.edu.jcu.codequest.domain.model.TopicAccuracy
import au.edu.jcu.codequest.domain.quiz.Scoring
import au.edu.jcu.codequest.ui.components.LoadingBox
import au.edu.jcu.codequest.ui.components.StarRating
import au.edu.jcu.codequest.ui.components.contentWidth
import java.text.DateFormat
import java.util.Date

@Composable
fun StatsRoute(viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(uiState)
}

/** User statistics: headline numbers, accuracy by topic and the most recent sessions. */
@Composable
fun StatsScreen(uiState: StatsUiState) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.stats_title)) }) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingBox(Modifier.padding(innerPadding))
        } else {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            value = stringResource(R.string.stats_lessons_value, uiState.completedLessons, uiState.totalLessons),
                            label = stringResource(R.string.stats_lessons_label),
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            value = uiState.totalXp.toString(),
                            label = stringResource(R.string.stats_xp_label),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            value = stringResource(R.string.stats_percent_value, uiState.firstTryPercent),
                            label = stringResource(R.string.stats_first_try_label),
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            value = uiState.daysPractised.toString(),
                            label = stringResource(R.string.stats_days_label),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    SectionTitle(stringResource(R.string.stats_adventure_title))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            value = stringResource(R.string.stats_lessons_value, uiState.missionsCompleted, uiState.missionsTotal),
                            label = stringResource(R.string.stats_missions_label),
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            value = stringResource(R.string.stats_lessons_value, uiState.flightsCompleted, uiState.flightsTotal),
                            label = stringResource(R.string.stats_flights_label),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    StatTile(
                        value = stringResource(R.string.stats_lessons_value, uiState.blocksCollected, uiState.blocksTotal),
                        label = stringResource(R.string.stats_blocks_label),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SectionTitle(stringResource(R.string.stats_topics_title))
                    TopicSection(uiState)
                    SectionTitle(stringResource(R.string.stats_recent_title))
                    if (uiState.recentSessions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.stats_recent_empty),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        uiState.recentSessions.forEach { session -> RecentSessionRow(session) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 12.dp)
            .semantics { heading() },
    )
}

/** One headline number. Read by TalkBack as a single phrase, e.g. "Total XP: 150". */
@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.stats_tile_description, label, value)
    Card(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TopicSection(uiState: StatsUiState) {
    when {
        !uiState.detailedHistory -> OutlinedCard(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.stats_topics_off),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
        uiState.topicAccuracy.isEmpty() -> Text(
            text = stringResource(R.string.stats_topics_empty),
            style = MaterialTheme.typography.bodyMedium,
        )
        else -> uiState.topicAccuracy.forEach { topic -> TopicRow(topic) }
    }
}

@Composable
private fun TopicRow(topic: TopicAccuracy) {
    val percent = Scoring.accuracyPercent(topic.correct, topic.total)
    val description = stringResource(R.string.stats_topic_description, topic.topic, topic.correct, topic.total)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row {
            Text(text = topic.topic, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.stats_topic_value, percent, topic.correct, topic.total),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LinearProgressIndicator(
            progress = { if (topic.total == 0) 0f else topic.correct.toFloat() / topic.total },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RecentSessionRow(session: RecentSession) {
    val formatter = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = session.lessonTitle, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = formatter.format(Date(session.finishedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.stats_recent_detail,
                        session.firstTryCorrect,
                        session.total,
                        session.xp,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            StarRating(stars = session.stars)
        }
    }
}
