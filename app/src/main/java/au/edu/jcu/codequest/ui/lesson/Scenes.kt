package au.edu.jcu.codequest.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.learn.Scene
import au.edu.jcu.codequest.ui.components.ByteMascot
import au.edu.jcu.codequest.ui.components.CodeTextStyle
import au.edu.jcu.codequest.ui.components.rememberReducedMotion
import au.edu.jcu.codequest.ui.theme.CodeFont
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme
import kotlin.random.Random

private val Amber = Color(0xFFFFB31A)
private val AmberDeep = Color(0xFFFF8A3D)
private val Danger = Color(0xFFFF5C6C)
private val Cyan = Color(0xFF7FE7FF)
private val SoftText = Color(0xFFCFC8F5)
private val Track = Color(0xFF2E2563)

/**
 * The little picture beside a teaching card's code, showing what the program does to Byte's
 * ship. It reacts as soon as the code changes. TalkBack hears a one-line summary instead.
 */
@Composable
fun SceneView(scene: Scene, modifier: Modifier = Modifier) {
    val space = CodeQuestTheme.colors.space
    val description = sceneDescription(scene)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(space.top, space.bottom)))
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Stars()
        when (scene) {
            is Scene.Fuel -> FuelScene(scene)
            is Scene.Types -> TypesScene(scene)
            is Scene.Radio -> RadioScene(scene)
            is Scene.Shields -> ShieldsScene(scene)
            is Scene.PlanetColor -> PlanetScene(scene)
            is Scene.Launch -> LaunchScene(scene)
            is Scene.Countdown -> CountdownScene(scene)
        }
    }
}

@Composable
private fun sceneDescription(scene: Scene): String = when (scene) {
    is Scene.Fuel -> stringResource(R.string.scene_fuel, scene.level, scene.capacity)
    is Scene.Types -> scene.values.joinToString { "${it.name}: ${it.type ?: stringResource(R.string.learn_type_error)}" }
    is Scene.Radio -> scene.message?.let { stringResource(R.string.scene_radio, it) } ?: stringResource(R.string.scene_radio_silent)
    is Scene.Shields -> stringResource(
        if (scene.raised) R.string.scene_shields_up else R.string.scene_cruising,
        scene.asteroids,
    )
    is Scene.PlanetColor -> stringResource(R.string.scene_planet, scene.planet, scene.color)
    is Scene.Launch -> if (scene.rockets.isEmpty()) {
        stringResource(R.string.scene_launch_none)
    } else {
        stringResource(R.string.scene_launch, scene.rockets.size, scene.rockets.joinToString())
    }
    is Scene.Countdown -> stringResource(R.string.scene_countdown, scene.display)
}

@Composable
private fun Stars() {
    val stars = remember {
        val random = Random(5)
        List(28) { Triple(random.nextFloat(), random.nextFloat(), 0.5f + random.nextFloat()) }
    }
    Canvas(Modifier.fillMaxSize()) {
        stars.forEach { (x, y, r) ->
            drawCircle(Color.White.copy(alpha = 0.55f), radius = r * density, center = Offset(x * size.width, y * size.height))
        }
    }
}

// ---------- Fuel ----------

@Composable
private fun FuelScene(scene: Scene.Fuel) {
    val overflow = scene.level > scene.capacity
    val fill by animateFloatAsState((scene.level.toFloat() / scene.capacity).coerceIn(0f, 1f), label = "fuel")
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.scene_fuel_label, scene.level, scene.capacity),
            style = CodeTextStyle.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
            color = if (overflow) Danger else Color.White,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Track)
                .border(2.dp, if (overflow) Danger else Color(0xFF4C409A), RoundedCornerShape(18.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fill)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (overflow) {
                            Brush.horizontalGradient(listOf(Danger, Color(0xFFFF9AA5)))
                        } else {
                            Brush.horizontalGradient(listOf(AmberDeep, Amber))
                        },
                    ),
            )
        }
        Text(
            text = stringResource(if (overflow) R.string.scene_overflow else if (scene.level == scene.capacity) R.string.scene_full else R.string.scene_engines),
            style = MaterialTheme.typography.labelLarge,
            color = if (overflow) Danger else SoftText,
        )
    }
}

// ---------- Types ----------

@Composable
private fun TypesScene(scene: Scene.Types) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        scene.values.forEach { value ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(value.name, style = CodeTextStyle.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.width(86.dp))
                Text(value.value ?: "?", style = CodeTextStyle, color = SoftText, modifier = Modifier.weight(1f))
                TypeBadge(value.type)
            }
        }
    }
}

@Composable
private fun TypeBadge(type: String?) {
    val colour = when (type) {
        "Int" -> Color(0xFF5FA8FF)
        "Double" -> Color(0xFFC9A7FF)
        "String" -> Color(0xFF7EE0A8)
        null -> Danger
        else -> Amber
    }
    Text(
        text = type ?: stringResource(R.string.learn_type_error),
        style = MaterialTheme.typography.labelLarge,
        color = Color(0xFF15112B),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colour)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

// ---------- Radio ----------

@Composable
private fun RadioScene(scene: Scene.Radio) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ByteMascot(Modifier.size(64.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = scene.message ?: "…",
                style = CodeTextStyle.copy(fontWeight = FontWeight.Bold),
                color = if (scene.message == null) Color(0xFF8D87AE) else Color(0xFF1D1838),
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                    .background(if (scene.message == null) Color.White.copy(alpha = 0.1f) else Color.White)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            Waveform(active = scene.message != null)
        }
    }
}

@Composable
private fun Waveform(active: Boolean) {
    val heights = remember { List(18) { i -> 0.25f + 0.75f * ((i * 37) % 11) / 10f } }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(22.dp),
    ) {
        val gap = size.width / heights.size
        heights.forEachIndexed { i, h ->
            val bar = if (active) h else 0.15f
            val x = gap * i + gap / 2
            drawLine(
                if (active) Color(0xFF7EE0A8) else Color(0xFF4C409A),
                Offset(x, size.height / 2 - bar * size.height / 2),
                Offset(x, size.height / 2 + bar * size.height / 2),
                strokeWidth = gap * 0.45f,
            )
        }
    }
}

// ---------- Shields ----------

@Composable
private fun ShieldsScene(scene: Scene.Shields) {
    val shield by animateFloatAsState(if (scene.raised) 1f else 0f, label = "shield")
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val ship = Offset(size.width * 0.26f, size.height * 0.5f)
            val random = Random(42)
            repeat(scene.asteroids) {
                val c = Offset(size.width * (0.5f + random.nextFloat() * 0.45f), size.height * (0.12f + random.nextFloat() * 0.7f))
                val r = (5f + random.nextFloat() * 7f) * density
                drawCircle(Color(0xFF6F6990), r, c)
                drawCircle(Color(0xFF8C86A8), r * 0.8f, c + Offset(-r * 0.15f, -r * 0.15f))
            }
            drawRocket(ship, height = size.height * 0.42f, angle = 90f, flame = true)
            if (shield > 0f) {
                val radius = size.height * 0.34f
                drawCircle(Cyan.copy(alpha = 0.14f * shield), radius, ship)
                drawCircle(Cyan.copy(alpha = 0.9f * shield), radius, ship, style = Stroke(width = 3.dp.toPx()))
            }
        }
        Text(
            text = stringResource(if (scene.raised) R.string.scene_shields_label else R.string.scene_cruising_label),
            style = MaterialTheme.typography.labelLarge,
            color = if (scene.raised) Cyan else SoftText,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        )
    }
}

// ---------- Planet colour ----------

@Composable
private fun PlanetScene(scene: Scene.PlanetColor) {
    val base = when (scene.color) {
        "red" -> Color(0xFFE5484D)
        "blue" -> Color(0xFF2F7CF6)
        "green" -> Color(0xFF12A06D)
        else -> Color(0xFF8C86A8)
    }
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Canvas(Modifier.size(110.dp)) {
            val r = size.minDimension / 2f
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.85f), base, base.copy(red = base.red * 0.5f, green = base.green * 0.5f, blue = base.blue * 0.5f)),
                    center = Offset(r * 0.7f, r * 0.6f),
                    radius = r * 1.5f,
                ),
                r,
            )
        }
        Column {
            Text(scene.planet, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text("colour = \"${scene.color}\"", style = CodeTextStyle, color = SoftText)
        }
    }
}

// ---------- Launch pad ----------

@Composable
private fun LaunchScene(scene: Scene.Launch) {
    val reducedMotion = rememberReducedMotion()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(scene.rockets) {
        progress.snapTo(0f)
        if (reducedMotion) progress.snapTo(1f) else progress.animateTo(1f, tween(900))
    }
    val measurer = rememberTextMeasurer()
    val numberStyle = TextStyle(fontFamily = CodeFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
    val emptyLabel = stringResource(R.string.scene_launch_none)
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val padTop = size.height - 22.dp.toPx()
            drawRect(Color(0xFF3A3170), topLeft = Offset(0f, padTop), size = Size(size.width, size.height - padTop))
            drawRect(Color(0xFF5B4FA0), topLeft = Offset(0f, padTop), size = Size(size.width, 3.dp.toPx()))
            val rockets = scene.rockets
            if (rockets.isEmpty()) return@Canvas
            val slot = size.width / rockets.size
            val rocketHeight = minOf(46.dp.toPx(), slot * 1.4f)
            rockets.forEachIndexed { i, number ->
                val lift = (padTop - rocketHeight - 18.dp.toPx()) * (0.25f + 0.75f * (i + 1) / rockets.size)
                val x = slot * i + slot / 2f
                val y = padTop - rocketHeight / 2f - 4.dp.toPx() - lift * progress.value
                drawRocket(Offset(x, y), rocketHeight, angle = 0f, flame = progress.value > 0.05f)
                val label = measurer.measure(number.toString(), numberStyle)
                drawText(label, topLeft = Offset(x - label.size.width / 2f, y + rocketHeight / 2f + 2.dp.toPx()))
            }
        }
        if (scene.rockets.isEmpty()) {
            Text(emptyLabel, style = MaterialTheme.typography.titleMedium, color = SoftText, modifier = Modifier.align(Alignment.Center))
        }
    }
}

// ---------- Countdown ----------

@Composable
private fun CountdownScene(scene: Scene.Countdown) {
    val lift by animateFloatAsState(if (scene.launched) 1f else 0f, tween(1000), label = "liftoff")
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val padTop = size.height - 18.dp.toPx()
            drawRect(Color(0xFF3A3170), topLeft = Offset(0f, padTop), size = Size(size.width, size.height - padTop))
            val h = 60.dp.toPx()
            val y = padTop - h / 2f - 4.dp.toPx() - lift * (size.height * 0.75f)
            drawRocket(Offset(size.width * 0.78f, y), h, angle = 0f, flame = scene.launched)
        }
        Text(
            text = scene.display,
            style = MaterialTheme.typography.displaySmall,
            color = if (scene.launched) Amber else Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp),
        )
    }
}

/** A small rocket centred on [centre], [height] tall, rotated by [angle] degrees (0 = pointing up). */
private fun DrawScope.drawRocket(centre: Offset, height: Float, angle: Float, flame: Boolean) {
    val w = height * 0.42f
    val top = centre.y - height / 2f
    rotate(angle, pivot = centre) {
        if (flame) {
            val flamePath = Path().apply {
                moveTo(centre.x - w * 0.22f, centre.y + height * 0.36f)
                quadraticTo(centre.x, centre.y + height * 0.72f, centre.x + w * 0.22f, centre.y + height * 0.36f)
                close()
            }
            drawPath(flamePath, Amber)
        }
        val body = Path().apply {
            moveTo(centre.x, top)
            cubicTo(centre.x + w * 0.6f, top + height * 0.2f, centre.x + w * 0.5f, top + height * 0.6f, centre.x + w * 0.42f, top + height * 0.84f)
            lineTo(centre.x - w * 0.42f, top + height * 0.84f)
            cubicTo(centre.x - w * 0.5f, top + height * 0.6f, centre.x - w * 0.6f, top + height * 0.2f, centre.x, top)
            close()
        }
        val fins = Path().apply {
            moveTo(centre.x - w * 0.38f, top + height * 0.55f)
            lineTo(centre.x - w * 0.8f, top + height * 0.9f)
            lineTo(centre.x - w * 0.38f, top + height * 0.84f)
            close()
            moveTo(centre.x + w * 0.38f, top + height * 0.55f)
            lineTo(centre.x + w * 0.8f, top + height * 0.9f)
            lineTo(centre.x + w * 0.38f, top + height * 0.84f)
            close()
        }
        drawPath(fins, Color(0xFFE5484D))
        drawPath(body, Color(0xFFF2EEFF))
        drawCircle(Color(0xFF7F52FF), radius = w * 0.2f, center = Offset(centre.x, top + height * 0.36f))
        drawRoundRect(
            Color(0xFF8C86A8),
            topLeft = Offset(centre.x - w * 0.3f, top + height * 0.84f),
            size = Size(w * 0.6f, height * 0.08f),
            cornerRadius = CornerRadius(w * 0.1f),
        )
    }
}
