package au.edu.jcu.codequest.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.edu.jcu.codequest.ui.theme.ChunkyColors
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme

/** How far a chunky element sits above its lower edge, and how far it sinks when pressed. */
val ChunkyDepth = 5.dp

/**
 * A raised surface with a darker lower edge, the app's signature "pressable" look.
 *
 * When [onClick] is set the whole surface is one button: it sinks onto its edge while pressed
 * and is announced as a single control by TalkBack. With [propagateMinConstraints] a caller's
 * fillMaxWidth() stretches the face as well as the edge.
 */
@Composable
fun ChunkySurface(
    colors: ChunkyColors,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    depth: Dp = ChunkyDepth,
    borderWidth: Dp = 0.dp,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClickLabel: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val sink by animateDpAsState(
        targetValue = if (pressed && enabled && onClick != null) depth else 0.dp,
        label = "chunkyPress",
    )
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Box(modifier = modifier.then(clickModifier), propagateMinConstraints = true) {
        // Lower edge, visible as a strip under the face.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = depth)
                .background(colors.edge, shape),
        )
        // Face, which slides down onto the edge while pressed.
        Box(
            modifier = Modifier
                .padding(top = sink, bottom = depth - sink)
                .clip(shape)
                .background(colors.face)
                .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, colors.border, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.content) {
                content()
            }
        }
    }
}

/** Full-height, chunky text button used for every main action in the app. */
@Composable
fun ChunkyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ChunkyColors = CodeQuestTheme.colors.primaryButton,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val shown = if (enabled) {
        colors
    } else {
        ChunkyColors(
            face = CodeQuestTheme.colors.lockFill,
            edge = CodeQuestTheme.colors.lockEdge,
            content = CodeQuestTheme.colors.lockInk,
            border = CodeQuestTheme.colors.lockFill,
        )
    }
    ChunkySurface(
        colors = shown,
        modifier = modifier.defaultMinSize(minHeight = height + ChunkyDepth),
        shape = RoundedCornerShape(16.dp),
        borderWidth = if (shown.border != shown.face) 2.dp else 0.dp,
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .height(height)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleMedium) {
                content()
            }
        }
    }
}

/** A thick rounded progress bar with a highlight stripe, used at the top of lessons. */
@Composable
fun ChunkyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = CodeQuestTheme.colors.successButton.face,
    trackColor: Color = CodeQuestTheme.colors.lockFill,
) {
    val fraction = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(trackColor),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color),
            ) {
                Box(
                    Modifier
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp)
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.35f)),
                )
            }
        }
    }
}
