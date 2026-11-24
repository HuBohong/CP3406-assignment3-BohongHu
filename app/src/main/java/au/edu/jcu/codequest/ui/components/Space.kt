package au.edu.jcu.codequest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme
import au.edu.jcu.codequest.ui.theme.SpaceColors
import kotlin.random.Random

// Everything on this page is drawn in code, so the art is sharp at any screen density,
// adds nothing to the APK and has no licensing questions.

private val ByteViolet = Color(0xFF7F52FF)
private val ByteEdge = Color(0xFF5A32D6)
private val ByteAmber = Color(0xFFFFB31A)
private val ByteInk = Color(0xFF1D1838)

/** Byte's expression, so the mascot can react to what just happened. */
enum class ByteMood { HAPPY, CHEERING, WORRIED }

/**
 * Byte, the app's robot guide. Decorative only: whatever Byte "says" is always also
 * written as text next to it, so nothing depends on seeing the drawing.
 */
@Composable
fun ByteMascot(modifier: Modifier = Modifier, mood: ByteMood = ByteMood.HAPPY) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)

        // Antenna
        drawLine(ByteEdge, p(50f, 6f), p(50f, 20f), strokeWidth = 5f * s, cap = StrokeCap.Round)
        drawCircle(ByteAmber, radius = 7f * s, center = p(50f, 7f))

        // Body, with a darker band along the bottom clipped to the body's rounded shape
        val body = Path().apply {
            addRoundRect(RoundRect(10f * s, 18f * s, 90f * s, 88f * s, CornerRadius(24f * s)))
        }
        drawPath(body, ByteViolet)
        clipPath(body) {
            drawRect(ByteEdge.copy(alpha = 0.55f), topLeft = p(10f, 76f), size = Size(80f * s, 12f * s))
        }

        // Arms, drawn as code brackets: < >
        val arm = Stroke(width = 5f * s, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(10f * s, 46f * s); lineTo(4f * s, 52f * s); lineTo(10f * s, 58f * s) }, ByteEdge, style = arm)
        drawPath(Path().apply { moveTo(90f * s, 46f * s); lineTo(96f * s, 52f * s); lineTo(90f * s, 58f * s) }, ByteEdge, style = arm)

        // Face screen
        drawRoundRect(Color.White, topLeft = p(20f, 30f), size = Size(60f * s, 40f * s), cornerRadius = CornerRadius(16f * s))

        // Eyes
        val eyeY = if (mood == ByteMood.WORRIED) 49f else 47f
        drawCircle(ByteInk, radius = 6f * s, center = p(38f, eyeY))
        drawCircle(ByteInk, radius = 6f * s, center = p(62f, eyeY))
        drawCircle(Color.White, radius = 2f * s, center = p(40f, eyeY - 2f))
        drawCircle(Color.White, radius = 2f * s, center = p(64f, eyeY - 2f))

        // Mouth
        val mouth = Path().apply {
            when (mood) {
                ByteMood.HAPPY -> { moveTo(42f * s, 58f * s); quadraticTo(50f * s, 65f * s, 58f * s, 58f * s) }
                ByteMood.CHEERING -> { moveTo(40f * s, 56f * s); quadraticTo(50f * s, 70f * s, 60f * s, 56f * s); close() }
                ByteMood.WORRIED -> { moveTo(43f * s, 63f * s); quadraticTo(50f * s, 58f * s, 57f * s, 63f * s) }
            }
        }
        if (mood == ByteMood.CHEERING) {
            drawPath(mouth, ByteInk)
        } else {
            drawPath(mouth, ByteInk, style = Stroke(width = 4f * s, cap = StrokeCap.Round))
        }
    }
}

/** One star of the background, in fractions of the canvas size so the field scales to any screen. */
private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

private fun makeStars(seed: Int, count: Int): List<Star> {
    val random = Random(seed)
    return List(count) {
        val big = random.nextFloat() > 0.85f
        Star(
            x = random.nextFloat(),
            y = random.nextFloat(),
            radius = if (big) 1.6f + random.nextFloat() * 0.9f else 0.6f + random.nextFloat() * 1.1f,
            alpha = 0.35f + random.nextFloat() * 0.6f,
        )
    }
}

/**
 * Deep-space backdrop: a violet gradient, two soft nebulae, a seeded star field and a few
 * four-point sparkles. The layout is fixed by [seed], so it looks the same on every launch.
 */
@Composable
fun SpaceBackground(modifier: Modifier = Modifier, seed: Int = 7) {
    val colors = CodeQuestTheme.colors.space
    val stars = remember(seed) { makeStars(seed, count = 110) }
    val sparkles = remember(seed) { makeStars(seed + 1, count = 7) }
    Canvas(modifier) {
        drawSpace(colors, stars, sparkles)
    }
}

private fun DrawScope.drawSpace(colors: SpaceColors, stars: List<Star>, sparkles: List<Star>) {
    drawRect(Brush.verticalGradient(listOf(colors.top, colors.middle, colors.bottom)))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(colors.nebulaPink.copy(alpha = 0.40f), Color.Transparent),
            center = Offset(size.width * 0.9f, size.height * 0.3f),
            radius = size.width * 0.6f,
        ),
        radius = size.width * 0.6f,
        center = Offset(size.width * 0.9f, size.height * 0.3f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(colors.nebulaBlue.copy(alpha = 0.35f), Color.Transparent),
            center = Offset(size.width * 0.05f, size.height * 0.8f),
            radius = size.width * 0.7f,
        ),
        radius = size.width * 0.7f,
        center = Offset(size.width * 0.05f, size.height * 0.8f),
    )
    val unit = density
    stars.forEach { star ->
        drawCircle(
            color = Color.White.copy(alpha = star.alpha),
            radius = star.radius * unit,
            center = Offset(star.x * size.width, star.y * size.height),
        )
    }
    sparkles.forEach { star ->
        val c = Offset(star.x * size.width, 0.1f * size.height + star.y * size.height * 0.85f)
        val k = (4f + star.radius * 2f) * unit
        val sparkle = Path().apply {
            moveTo(c.x, c.y - k)
            quadraticTo(c.x, c.y, c.x + k, c.y)
            quadraticTo(c.x, c.y, c.x, c.y + k)
            quadraticTo(c.x, c.y, c.x - k, c.y)
            quadraticTo(c.x, c.y, c.x, c.y - k)
            close()
        }
        drawPath(sparkle, Color.White.copy(alpha = 0.85f))
    }
}

/** The look of each unit's planet on the map. */
enum class PlanetStyle(
    val light: Color,
    val base: Color,
    val dark: Color,
    val hasRing: Boolean,
    val hasCraters: Boolean,
) {
    RINGED_VIOLET(Color(0xFFC9B3FF), Color(0xFF7F52FF), Color(0xFF3F1FA8), hasRing = true, hasCraters = false),
    CRATERED_BLUE(Color(0xFF9CD3FF), Color(0xFF2F7CF6), Color(0xFF123C8C), hasRing = false, hasCraters = true),
    OCEAN_GREEN(Color(0xFF9EF0C2), Color(0xFF12A06D), Color(0xFF0A5C40), hasRing = false, hasCraters = false),
    RINGED_MAGENTA(Color(0xFFF3B3FF), Color(0xFFC83DD8), Color(0xFF6E1580), hasRing = true, hasCraters = true),
    ;

    companion object {
        fun forUnit(unitIndex: Int): PlanetStyle = entries[(unitIndex - 1).mod(entries.size)]
    }
}

/** A small decorative planet; [size][Modifier.size] it, the drawing fills the space. */
@Composable
fun Planet(style: PlanetStyle, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val ringRoom = if (style.hasRing) 0.62f else 1f
        val radius = size.minDimension / 2f * ringRoom
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to style.light,
                0.6f to style.base,
                1f to style.dark,
                center = Offset(center.x - radius * 0.3f, center.y - radius * 0.4f),
                radius = radius * 1.4f,
            ),
            radius = radius,
            center = center,
        )
        if (style.hasCraters) {
            val crater = style.dark.copy(alpha = 0.45f)
            drawCircle(crater, radius * 0.16f, Offset(center.x - radius * 0.28f, center.y - radius * 0.2f))
            drawCircle(crater, radius * 0.11f, Offset(center.x + radius * 0.3f, center.y + radius * 0.28f))
            drawCircle(crater, radius * 0.08f, Offset(center.x + radius * 0.18f, center.y - radius * 0.48f))
        }
        if (style.hasRing) {
            withTransform({ rotate(-18f, pivot = center) }) {
                drawOval(
                    color = style.light.copy(alpha = 0.8f),
                    topLeft = Offset(center.x - radius * 1.7f, center.y - radius * 0.4f),
                    size = Size(radius * 3.4f, radius * 0.8f),
                    style = Stroke(width = radius * 0.13f),
                )
            }
        }
    }
}
