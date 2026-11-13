package au.edu.jcu.codequest.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.quiz.Scoring

/** Widest a column of content may grow, so text lines stay readable on tablets. */
val MaxContentWidth = 640.dp

/** Fills the available width up to [MaxContentWidth]. Use inside a centred parent. */
fun Modifier.contentWidth(): Modifier = this.widthIn(max = MaxContentWidth).fillMaxWidth()

/** A snippet of Kotlin in a monospace font. Long lines scroll sideways instead of wrapping. */
@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
        )
    }
}

/**
 * Star rating out of [Scoring.MAX_STARS]. Screen readers hear one sentence
 * ("2 of 3 stars") instead of three separate icons.
 */
@Composable
fun StarRating(stars: Int, modifier: Modifier = Modifier, starSize: Dp = 20.dp) {
    val description = stringResource(R.string.stars_description, stars, Scoring.MAX_STARS)
    Row(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        repeat(Scoring.MAX_STARS) { index ->
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (index < stars) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                modifier = Modifier.size(starSize),
            )
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
