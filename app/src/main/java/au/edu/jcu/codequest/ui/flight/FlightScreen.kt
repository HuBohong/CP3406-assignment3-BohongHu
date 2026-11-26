package au.edu.jcu.codequest.ui.flight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.flight.FlightLevel
import au.edu.jcu.codequest.domain.flight.FlightOutcome
import au.edu.jcu.codequest.domain.flight.FlightResult
import au.edu.jcu.codequest.domain.flight.Point
import au.edu.jcu.codequest.ui.components.ByteMascot
import au.edu.jcu.codequest.ui.components.ByteMood
import au.edu.jcu.codequest.ui.components.ChunkyButton
import au.edu.jcu.codequest.ui.components.LightSystemBarIcons
import au.edu.jcu.codequest.ui.components.SpaceBackground
import au.edu.jcu.codequest.ui.components.contentWidth
import au.edu.jcu.codequest.ui.theme.CodeFont
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme
import kotlin.math.atan2

object FlightTestTags {
    const val LAUNCH = "flight_launch"
    const val RESULT = "flight_result"
    fun dial(name: String, direction: Int) = "flight_${name}_${if (direction > 0) "up" else "down"}"
}

private val CodeLine = TextStyle(fontFamily = CodeFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
private val CodeInk = Color(0xFF1D1838)
private val ParamInk = Color(0xFF5A32D6)
private val TrailColor = Color(0xFFFFC94D)
private val GhostColor = Color(0x80FFFFFF)
private val PadColor = Color(0xFF8FF0D0)
private val AsteroidColor = Color(0xFF8C86A8)
private val AsteroidShade = Color(0xFF5E5878)
private val WormholeColors = listOf(Color(0xFF3A1078), Color(0xFF7B2CBF), Color(0xFF240046), Color(0xFFC77DFF))

/** Width to height of the sky scene, matching its portrait proportions. */
private const val SKY_ASPECT_RATIO = 1.2f

/** Widest the two-pane landscape layout grows on large screens. */
private val LandscapeMaxWidth = 1100.dp

@Composable
fun FlightRoute(onExit: () -> Unit, viewModel: FlightViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FlightScreen(
        uiState = uiState,
        onExit = onExit,
        onChangeAngle = viewModel::changeAngle,
        onChangePower = viewModel::changePower,
        onLaunch = viewModel::launch,
        onAdjust = viewModel::adjust,
    )
}

/**
 * A rocket flight between two planets. The learner sets the two arguments of
 * `launch(angle, power)`, presses Launch and watches the parabola; a miss falls into a wormhole
 * and the last path stays on screen as a guide for the next try.
 */
@Composable
fun FlightScreen(
    uiState: FlightUiState,
    onExit: () -> Unit,
    onChangeAngle: (Int) -> Unit,
    onChangePower: (Int) -> Unit,
    onLaunch: () -> Unit,
    onAdjust: () -> Unit,
) {
    LightSystemBarIcons()
    val space = CodeQuestTheme.colors.space
    val level = uiState.level
    Box(Modifier.fillMaxSize()) {
        SpaceBackground(Modifier.matchParentSize(), seed = 31)
        if (level == null) {
            Text(
                text = stringResource(R.string.flight_not_found),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )
            return@Box
        }
        val configuration = LocalConfiguration.current
        // In landscape the sky sits beside the launch code instead of above it.
        val landscape = configuration.screenWidthDp > configuration.screenHeightDp
        val skyHeight = (configuration.screenHeightDp.dp * 0.38f).coerceIn(180.dp, 320.dp)
        val sky: @Composable (Modifier) -> Unit = { skyModifier ->
            FlightSky(
                level = level,
                aimAngle = uiState.angle,
                flight = uiState.flight,
                shownPoints = uiState.shownPoints,
                previous = uiState.previous,
                modifier = skyModifier,
            )
        }
        val launchControls: @Composable () -> Unit = {
            Text(text = level.goal, style = MaterialTheme.typography.bodyMedium, color = space.textSoft)
            LaunchCode(
                angle = uiState.angle,
                power = uiState.power,
                enabled = uiState.canChangeValues,
                onChangeAngle = onChangeAngle,
                onChangePower = onChangePower,
            )
            if (uiState.launches > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(space.glass)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ByteMascot(Modifier.size(32.dp))
                    Text(text = level.hint, style = MaterialTheme.typography.bodyMedium, color = space.text)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        val launchButton: @Composable (Modifier) -> Unit = { buttonModifier ->
            ChunkyButton(
                onClick = onLaunch,
                enabled = uiState.canChangeValues,
                colors = CodeQuestTheme.colors.successButton,
                modifier = buttonModifier.testTag(FlightTestTags.LAUNCH),
            ) {
                Text(stringResource(if (uiState.isFlying) R.string.flight_flying else R.string.flight_launch))
            }
        }
        val paneWidth = if (landscape) Modifier.widthIn(max = LandscapeMaxWidth).fillMaxWidth() else Modifier.contentWidth()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = paneWidth
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onExit) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.flight_close), tint = space.textSoft)
                }
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
                    Text(
                        text = stringResource(R.string.flight_label, level.fromUnit + 1),
                        style = MaterialTheme.typography.labelMedium,
                        color = space.textSoft,
                    )
                    Text(text = level.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
            if (landscape) {
                Row(
                    modifier = paneWidth
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(bottom = 10.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        // Same proportions as in portrait, so the scene is never cropped.
                        sky(Modifier.fillMaxHeight().aspectRatio(SKY_ASPECT_RATIO, matchHeightConstraintsFirst = true))
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            launchControls()
                        }
                        launchButton(Modifier.fillMaxWidth().padding(bottom = 10.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .contentWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    sky(Modifier.fillMaxWidth().height(skyHeight))
                    launchControls()
                }
                launchButton(
                    Modifier
                        .contentWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
        AnimatedVisibility(
            visible = uiState.outcome != null,
            enter = slideInVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            uiState.outcome?.let { outcome ->
                FlightResultPanel(outcome = outcome, nextPlanet = level.fromUnit + 1, onBackToMap = onExit, onAdjust = onAdjust)
            }
        }
    }
}

/** `launch(angle = …, power = …)` with a − and + button on each argument. */
@Composable
private fun LaunchCode(
    angle: Int,
    power: Int,
    enabled: Boolean,
    onChangeAngle: (Int) -> Unit,
    onChangePower: (Int) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        contentColor = CodeInk,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("launch(", style = CodeLine, color = CodeInk)
            ArgumentRow(name = "angle", value = angle, unit = "°", enabled = enabled, onChange = onChangeAngle)
            ArgumentRow(name = "power", value = power, unit = "", enabled = enabled, onChange = onChangePower)
            Text(")", style = CodeLine, color = CodeInk)
        }
    }
}

@Composable
private fun ArgumentRow(name: String, value: Int, unit: String, enabled: Boolean, onChange: (Int) -> Unit) {
    val description = stringResource(R.string.flight_argument_description, name, value)
    Row(
        modifier = Modifier.padding(start = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$name = ", style = CodeLine, color = CodeInk, modifier = Modifier.clearAndSetSemantics {})
        DialButton(label = "−", enabled = enabled, testTag = FlightTestTags.dial(name, -1)) { onChange(-1) }
        Text(
            text = "$value$unit",
            style = CodeLine,
            color = ParamInk,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1ECFF))
                .border(2.dp, Color(0xFF7F52FF), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp)
                .semantics {
                    contentDescription = description
                    liveRegion = LiveRegionMode.Polite
                },
        )
        DialButton(label = "+", enabled = enabled, testTag = FlightTestTags.dial(name, +1)) { onChange(+1) }
        Text(",", style = CodeLine, color = CodeInk)
    }
}

@Composable
private fun DialButton(label: String, enabled: Boolean, testTag: String, onClick: () -> Unit) {
    val description = stringResource(if (label == "+") R.string.flight_more else R.string.flight_less)
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .testTag(testTag)
            .semantics { contentDescription = description },
    ) {
        Text(label, style = CodeLine, color = if (enabled) ParamInk else Color(0xFFB9B3D1))
    }
}

/**
 * The space between the planets: the launch pad on the left, the next planet's landing pad on
 * the right, asteroids in between and wormholes underneath. World units are scaled to fit the
 * width; y = 0 is the level of both pads.
 */
@Composable
private fun FlightSky(
    level: FlightLevel,
    aimAngle: Int,
    flight: FlightResult?,
    shownPoints: Int,
    previous: FlightResult?,
    modifier: Modifier = Modifier,
) {
    val description = flightDescription(level, flight, shownPoints)
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x40000000))
            .clipToBounds()
            .semantics { contentDescription = description },
    ) {
        val scale = size.width / (level.width.toFloat() + LEFT_MARGIN)
        val groundY = size.height * 0.72f
        fun at(point: Point) = Offset((LEFT_MARGIN + point.x.toFloat()) * scale, groundY - point.y.toFloat() * scale)

        drawWormhole(Offset(at(Point(level.distance / 2, 0.0)).x, groundY + (size.height - groundY) * 0.6f), size.width * 0.16f)
        drawWormhole(Offset(size.width * 0.97f, groundY + (size.height - groundY) * 0.6f), size.width * 0.07f)

        // Launch planet with Byte's pad at the origin.
        val home = at(Point(0.0, 0.0))
        drawCircle(Color(0xFF7F52FF), radius = 26f * scale, center = Offset(home.x, groundY + 26f * scale * 0.9f))
        drawLine(Color.White, Offset(home.x - 10f * scale, groundY), Offset(home.x + 10f * scale, groundY), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)

        // Next planet with its landing pad.
        val padRadius = level.padRadius.toFloat() * scale
        val target = at(Point(level.distance, 0.0))
        drawCircle(Color(0xFF12A06D), radius = padRadius * 1.4f, center = Offset(target.x, groundY + padRadius * 1.3f))
        drawLine(PadColor, Offset(target.x - padRadius, groundY), Offset(target.x + padRadius, groundY), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)

        level.asteroids.forEach { rock ->
            val centre = at(Point(rock.x, rock.y))
            val r = rock.radius.toFloat() * scale
            drawCircle(AsteroidColor, r, centre)
            drawCircle(AsteroidShade, r * 0.28f, centre + Offset(-r * 0.3f, -r * 0.2f))
            drawCircle(AsteroidShade, r * 0.18f, centre + Offset(r * 0.35f, r * 0.3f))
        }

        previous?.let { ghost ->
            drawPath(
                path = ghost.path.toPath(::at),
                color = GhostColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f))),
            )
        }

        if (flight != null && shownPoints > 0) {
            val shown = flight.path.take(shownPoints)
            drawPath(path = shown.toPath(::at), color = TrailColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            val tip = at(shown.last())
            val before = at(shown[(shown.size - 2).coerceAtLeast(0)])
            val finished = shownPoints >= flight.path.size
            when {
                finished && flight.outcome == FlightOutcome.HIT_ASTEROID -> drawBurst(tip, 10.dp.toPx())
                finished && flight.outcome != FlightOutcome.LANDED -> drawWormhole(Offset(tip.x, tip.y + 6.dp.toPx()), 14.dp.toPx())
                else -> drawRocket(tip, heading = atan2(tip.y - before.y, tip.x - before.x), size = 9.dp.toPx())
            }
        } else {
            // Waiting on the pad, tilted to the angle the learner has set.
            val aim = -Math.toRadians(aimAngle.toDouble()).toFloat()
            drawRocket(Offset(home.x, groundY - 6.dp.toPx()), heading = aim, size = 9.dp.toPx())
        }
    }
}

private const val LEFT_MARGIN = 30f

private fun List<Point>.toPath(at: (Point) -> Offset): Path = Path().apply {
    forEachIndexed { index, point ->
        val p = at(point)
        if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
}

/** A small rocket pointing along [heading] (radians, canvas coordinates). */
private fun DrawScope.drawRocket(tip: Offset, heading: Float, size: Float) {
    rotate(degrees = Math.toDegrees(heading.toDouble()).toFloat(), pivot = tip) {
        val body = Path().apply {
            moveTo(tip.x + size, tip.y)
            lineTo(tip.x - size, tip.y - size * 0.55f)
            lineTo(tip.x - size * 0.6f, tip.y)
            lineTo(tip.x - size, tip.y + size * 0.55f)
            close()
        }
        drawPath(body, Color.White)
        drawCircle(Color(0xFFFF8A3D), size * 0.35f, Offset(tip.x - size * 0.95f, tip.y))
    }
}

/** A swirl of rings: where missed rockets end up. */
private fun DrawScope.drawWormhole(centre: Offset, radius: Float) {
    for (i in 0 until 4) {
        val r = radius * (1f - i * 0.22f)
        drawOval(
            color = WormholeColors[i],
            topLeft = Offset(centre.x - r, centre.y - r * 0.45f),
            size = Size(r * 2, r * 0.9f),
        )
    }
}

private fun DrawScope.drawBurst(centre: Offset, radius: Float) {
    drawCircle(Color(0xFFFF8A3D), radius, centre)
    drawCircle(Color(0xFFFFE3A3), radius * 0.5f, centre)
}

@Composable
private fun flightDescription(level: FlightLevel, flight: FlightResult?, shownPoints: Int): String {
    val outcome = flight?.takeIf { shownPoints >= it.path.size }?.outcome
    return when (outcome) {
        null -> stringResource(R.string.flight_sky_description, level.distance.toInt(), level.asteroids.size)
        else -> outcomeTitle(outcome)
    }
}

@Composable
private fun outcomeTitle(outcome: FlightOutcome): String = stringResource(
    when (outcome) {
        FlightOutcome.LANDED -> R.string.flight_landed_title
        FlightOutcome.TOO_SHORT -> R.string.flight_short_title
        FlightOutcome.TOO_FAR -> R.string.flight_far_title
        FlightOutcome.HIT_ASTEROID -> R.string.flight_asteroid_title
    },
)

@Composable
private fun FlightResultPanel(outcome: FlightOutcome, nextPlanet: Int, onBackToMap: () -> Unit, onAdjust: () -> Unit) {
    val landed = outcome == FlightOutcome.LANDED
    val extra = CodeQuestTheme.colors
    val background = if (landed) extra.successContainer else MaterialTheme.colorScheme.errorContainer
    val textColor = if (landed) extra.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
    Surface(
        color = background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        // A centred sheet on wide screens and in landscape instead of a full-width strip.
        modifier = Modifier
            .contentWidth()
            .testTag(FlightTestTags.RESULT),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(20.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ByteMascot(Modifier.size(52.dp), mood = if (landed) ByteMood.CHEERING else ByteMood.WORRIED)
                Text(
                    text = outcomeTitle(outcome),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (landed) extra.success else MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = when (outcome) {
                    FlightOutcome.LANDED -> stringResource(R.string.flight_landed_body, nextPlanet)
                    FlightOutcome.TOO_SHORT -> stringResource(R.string.flight_short_body)
                    FlightOutcome.TOO_FAR -> stringResource(R.string.flight_far_body)
                    FlightOutcome.HIT_ASTEROID -> stringResource(R.string.flight_asteroid_body)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
            )
            if (landed) {
                ChunkyButton(onClick = onBackToMap, colors = extra.successButton, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.mission_back_to_map))
                }
                TextButton(onClick = onAdjust, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.flight_fly_again), color = textColor)
                }
            } else {
                ChunkyButton(onClick = onAdjust, colors = extra.dangerButton, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.flight_adjust))
                }
            }
        }
    }
}
