package au.edu.jcu.codequest.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.quiz.Scoring
import au.edu.jcu.codequest.ui.theme.CodeFont
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme
import au.edu.jcu.codequest.ui.theme.EditorColors

/** Widest a column of content may grow, so text lines stay readable on tablets. */
val MaxContentWidth = 640.dp

/** Fills the available width up to [MaxContentWidth]. Use inside a centred parent. */
fun Modifier.contentWidth(): Modifier = this.widthIn(max = MaxContentWidth).fillMaxWidth()

/** Text style for code, shared by the editor, answer options and ordering lines. */
val CodeTextStyle = TextStyle(fontFamily = CodeFont, fontSize = 15.sp, lineHeight = 26.sp)

/**
 * A dark code editor with line numbers and Kotlin syntax colours. Long lines scroll sideways
 * instead of wrapping, so the indentation that matters in code is never broken.
 *
 * @param highlightedLine 1-based line to mark as "running now", or null.
 */
@Composable
fun CodeEditor(
    code: String,
    modifier: Modifier = Modifier,
    highlightedLine: Int? = null,
) {
    val colors = CodeQuestTheme.colors.editor
    val lines = remember(code) { code.lines() }
    val highlighted = remember(code, colors) { lines.map { highlightKotlin(it, colors) } }
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.background)
            .padding(vertical = 12.dp),
    ) {
        val density = LocalDensity.current
        val textStyle = remember(lines, maxWidth, density) {
            fitCodeStyle(measurer, lines, available = with(density) { (maxWidth - CodeGutter).toPx() })
        }
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                // At least as wide as the editor, so the current-line highlight spans it.
                .widthIn(min = maxWidth)
                .width(IntrinsicSize.Max),
        ) {
            highlighted.forEachIndexed { index, line ->
                val isCurrent = highlightedLine == index + 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(if (isCurrent) colors.currentLine else Color.Transparent),
                ) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(if (isCurrent) colors.currentLineMarker else Color.Transparent),
                    )
                    Text(
                        text = (index + 1).toString(),
                        style = textStyle,
                        color = colors.lineNumber,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(30.dp),
                    )
                    Text(
                        text = line,
                        style = textStyle,
                        color = colors.text,
                        softWrap = false,
                        modifier = Modifier.padding(start = 14.dp, end = 16.dp),
                    )
                }
            }
        }
    }
}

/** Room each editor line needs besides its text: current-line marker, line number and padding. */
private val CodeGutter = 4.dp + 30.dp + 14.dp + 16.dp

/** The code font never shrinks below this share of its normal size; longer lines scroll sideways. */
private const val MinCodeScale = 0.8f

/** Shrinks the code font a little when the longest line would not fit, so short lines need no sideways scrolling. */
private fun fitCodeStyle(measurer: TextMeasurer, lines: List<String>, available: Float): TextStyle {
    val longest = lines.maxByOrNull { it.length }.orEmpty()
    if (longest.isEmpty() || available <= 0f) return CodeTextStyle
    val natural = measurer.measure(longest, CodeTextStyle, softWrap = false, maxLines = 1).size.width.toFloat()
    if (natural <= available) return CodeTextStyle
    val scale = (available / natural).coerceAtLeast(MinCodeScale)
    return CodeTextStyle.copy(fontSize = CodeTextStyle.fontSize * scale, lineHeight = CodeTextStyle.lineHeight * scale)
}

/** Colours one line of Kotlin with the editor palette. */
fun highlightKotlin(line: String, colors: EditorColors): AnnotatedString = buildAnnotatedString {
    KotlinTokenizer.tokenize(line).forEach { token ->
        val color = when (token.kind) {
            TokenKind.KEYWORD -> colors.keyword
            TokenKind.NUMBER -> colors.number
            TokenKind.STRING -> colors.string
            TokenKind.FUNCTION -> colors.function
            TokenKind.COMMENT -> colors.comment
            TokenKind.PLAIN -> colors.text
        }
        withStyle(SpanStyle(color = color)) { append(token.text) }
    }
}

/**
 * Star rating out of [Scoring.MAX_STARS]. Screen readers hear one sentence
 * ("2 of 3 stars") instead of three separate icons.
 */
@Composable
fun StarRating(
    stars: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 20.dp,
    emptyColor: Color = CodeQuestTheme.colors.lockEdge,
) {
    val description = stringResource(R.string.stars_description, stars, Scoring.MAX_STARS)
    Row(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        repeat(Scoring.MAX_STARS) { index ->
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (index < stars) CodeQuestTheme.colors.xp else emptyColor,
                modifier = Modifier.size(starSize),
            )
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (color == Color.Unspecified) CircularProgressIndicator() else CircularProgressIndicator(color = color)
    }
}

/**
 * While this is on screen, status- and navigation-bar icons are drawn light, for screens
 * with a dark space background. The previous appearance is restored when it leaves.
 */
@Composable
fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose {}
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previousStatus = controller.isAppearanceLightStatusBars
            val previousNavigation = controller.isAppearanceLightNavigationBars
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
            onDispose {
                controller.isAppearanceLightStatusBars = previousStatus
                controller.isAppearanceLightNavigationBars = previousNavigation
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
