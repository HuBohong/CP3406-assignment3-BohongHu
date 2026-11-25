package au.edu.jcu.codequest.ui.mission

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.edu.jcu.codequest.domain.mission.BossPhase
import au.edu.jcu.codequest.domain.mission.BossState
import au.edu.jcu.codequest.domain.mission.Cell
import au.edu.jcu.codequest.domain.mission.GridState
import au.edu.jcu.codequest.domain.mission.Heading
import au.edu.jcu.codequest.domain.mission.Tile
import au.edu.jcu.codequest.ui.components.ByteMascot
import au.edu.jcu.codequest.ui.components.ByteMood

private val BoardFrame = Color(0xFF2A1F66)
private val FloorTop = Color(0xFF4B3F9A)
private val FloorEdge = Color(0xFF352A78)
private val RockColor = Color(0xFF8C86A8)
private val RockShade = Color(0xFF5E5878)
private val CraterColor = Color(0xFF120D33)
private val GoalColor = Color(0xFF1E6E57)
private val GoalEdge = Color(0xFF155443)
private val Beacon = Color(0xFF8FF0D0)
private val CrystalLight = Color(0xFF8FF5E4)
private val CrystalDark = Color(0xFF1E8F7E)
private val TrailColor = Color(0xFFB79CFF)
private val LaserCore = Color(0xFFFFE3EE)
private val LaserGlow = Color(0xFFFF4D8D)
private val DroneBody = Color(0xFFB02CC4)
private val DroneEdge = Color(0xFF6E1580)
private val HeadingPointer = Color(0xFFFFC94D)

/**
 * A grid planet: tiles, crystals, the beacon and Byte. Byte glides between squares and
 * shows which way it faces with a small arrow. The board describes itself to TalkBack.
 *
 * Tiles are square and as large as the width allows, but the board never grows taller than
 * [maxBoardHeight], so tall maps shrink instead of spilling over the screen around them.
 */
@Composable
fun GridBoard(
    state: GridState,
    description: String,
    modifier: Modifier = Modifier,
    maxBoardHeight: Dp = 300.dp,
) {
    val level = state.level
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val tile: Dp = minOf(maxWidth / level.width, maxBoardHeight / level.height)
        val boardWidth = tile * level.width
        val boardHeight = tile * level.height
        Box(Modifier.size(boardWidth, boardHeight)) {
            Canvas(Modifier.size(boardWidth, boardHeight)) {
                drawGrid(state, tile.toPx())
            }
            val x by animateDpAsState(tile * state.byte.x, label = "byteX")
            val y by animateDpAsState(tile * state.byte.y, label = "byteY")
            val fell = level.tileAt(state.byte) == Tile.CRATER
            val scale by animateFloatAsState(if (fell) 0.35f else 1f, label = "byteFall")
            Box(
                modifier = Modifier
                    .offset(x, y)
                    .size(tile),
                contentAlignment = Alignment.Center,
            ) {
                ByteMascot(
                    modifier = Modifier
                        .size(tile * 0.78f * scale)
                        .alpha(if (fell) 0.6f else 1f),
                    mood = if (fell) ByteMood.WORRIED else ByteMood.HAPPY,
                )
                // Small amber pointer showing which way Byte faces (drawn pointing right, then rotated).
                Canvas(
                    Modifier
                        .align(headingAlignment(state.heading))
                        .size(tile * 0.26f)
                        .rotate(headingDegrees(state.heading)),
                ) {
                    drawPath(
                        Path().apply {
                            moveTo(size.width * 0.15f, size.height * 0.1f)
                            lineTo(size.width * 0.95f, size.height * 0.5f)
                            lineTo(size.width * 0.15f, size.height * 0.9f)
                            close()
                        },
                        HeadingPointer,
                    )
                }
            }
        }
    }
}

private fun headingDegrees(heading: Heading): Float = when (heading) {
    Heading.EAST -> 0f
    Heading.SOUTH -> 90f
    Heading.WEST -> 180f
    Heading.NORTH -> 270f
}

private fun headingAlignment(heading: Heading): Alignment = when (heading) {
    Heading.EAST -> Alignment.CenterEnd
    Heading.SOUTH -> Alignment.BottomCenter
    Heading.WEST -> Alignment.CenterStart
    Heading.NORTH -> Alignment.TopCenter
}

private fun DrawScope.drawGrid(state: GridState, tile: Float) {
    val level = state.level
    val gap = tile * 0.06f
    val radius = CornerRadius(tile * 0.16f)
    drawRoundRect(BoardFrame, size = size, cornerRadius = CornerRadius(tile * 0.25f))
    for (y in 0 until level.height) {
        for (x in 0 until level.width) {
            val cell = Cell(x, y)
            val topLeft = Offset(x * tile + gap, y * tile + gap)
            val cellSize = Size(tile - 2 * gap, tile - 2 * gap)
            when (level.tileAt(cell)) {
                Tile.FLOOR -> drawTile(topLeft, cellSize, radius, FloorTop, FloorEdge)
                Tile.GOAL -> {
                    drawTile(topLeft, cellSize, radius, GoalColor, GoalEdge)
                    drawBeacon(topLeft, cellSize)
                }
                Tile.ROCK -> {
                    drawTile(topLeft, cellSize, radius, FloorTop, FloorEdge)
                    drawRock(topLeft, cellSize)
                }
                Tile.CRATER -> {
                    drawRoundRect(CraterColor, topLeft, cellSize, radius)
                    drawOval(
                        Color.Black.copy(alpha = 0.35f),
                        topLeft = topLeft + Offset(cellSize.width * 0.15f, cellSize.height * 0.2f),
                        size = Size(cellSize.width * 0.7f, cellSize.height * 0.55f),
                    )
                }
                null -> Unit
            }
        }
    }
    // Breadcrumbs where Byte has been.
    state.trail.dropLast(1).forEach { cell ->
        drawCircle(TrailColor.copy(alpha = 0.7f), radius = tile * 0.07f, center = centreOf(cell, tile))
    }
    state.crystalsLeft.forEach { cell -> drawCrystal(centreOf(cell, tile), tile * 0.26f) }
}

private fun centreOf(cell: Cell, tile: Float) = Offset((cell.x + 0.5f) * tile, (cell.y + 0.5f) * tile)

private fun DrawScope.drawTile(topLeft: Offset, size: Size, radius: CornerRadius, face: Color, edge: Color) {
    drawRoundRect(edge, topLeft, size, radius)
    drawRoundRect(face, topLeft, Size(size.width, size.height * 0.86f), radius)
}

private fun DrawScope.drawRock(topLeft: Offset, size: Size) {
    val c = topLeft + Offset(size.width / 2f, size.height * 0.55f)
    val r = size.minDimension * 0.34f
    drawCircle(RockShade, r, c + Offset(0f, r * 0.15f))
    drawCircle(RockColor, r * 0.92f, c)
    drawCircle(Color.White.copy(alpha = 0.25f), r * 0.28f, c + Offset(-r * 0.35f, -r * 0.35f))
}

private fun DrawScope.drawBeacon(topLeft: Offset, size: Size) {
    val left = topLeft.x + size.width * 0.34f
    val top = topLeft.y + size.height * 0.2f
    val bottom = topLeft.y + size.height * 0.78f
    val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
    drawLine(Beacon, Offset(left, top), Offset(left, bottom), strokeWidth = stroke.width, cap = StrokeCap.Round)
    val flag = Path().apply {
        moveTo(left, top)
        lineTo(left + size.width * 0.36f, top + size.height * 0.11f)
        lineTo(left, top + size.height * 0.24f)
        close()
    }
    drawPath(flag, Beacon)
}

private fun DrawScope.drawCrystal(centre: Offset, r: Float) {
    val gem = Path().apply {
        moveTo(centre.x - r * 0.75f, centre.y - r * 0.45f)
        lineTo(centre.x + r * 0.75f, centre.y - r * 0.45f)
        lineTo(centre.x + r, centre.y - r * 0.05f)
        lineTo(centre.x, centre.y + r)
        lineTo(centre.x - r, centre.y - r * 0.05f)
        close()
    }
    drawPath(gem, Brush.verticalGradient(listOf(CrystalLight, CrystalDark), startY = centre.y - r, endY = centre.y + r))
    drawLine(Color.White.copy(alpha = 0.6f), Offset(centre.x - r * 0.4f, centre.y - r * 0.2f), Offset(centre.x, centre.y + r * 0.6f), strokeWidth = r * 0.12f)
}

/**
 * The boss arena: three lanes, Byte on the left and the laser drone on the right. Lanes being
 * charged glow red; when the drone fires, a beam crosses them.
 */
@Composable
fun BossArena(
    state: BossState,
    description: String,
    modifier: Modifier = Modifier,
    maxBoardHeight: Dp = 260.dp,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        // A wide arena (1.6 : 1) that gets shorter rather than overflowing on short screens.
        val arenaHeight = minOf(maxWidth / 1.6f, maxBoardHeight)
        val laneHeight = arenaHeight / state.level.lanes
        Box(Modifier.size(maxWidth, arenaHeight)) {
            Canvas(Modifier.size(maxWidth, arenaHeight)) {
                drawArena(state, laneHeight.toPx())
            }
            val byteY by animateDpAsState(laneHeight * state.byteLane, label = "byteLane")
            val hit = state.phase == BossPhase.FIRED && state.hitThisTurn
            Box(
                modifier = Modifier
                    .offset(x = 12.dp, y = byteY)
                    .size(laneHeight),
                contentAlignment = Alignment.Center,
            ) {
                ByteMascot(
                    modifier = Modifier.size(laneHeight * 0.8f),
                    mood = if (hit) ByteMood.WORRIED else ByteMood.HAPPY,
                )
            }
        }
    }
}

private fun DrawScope.drawArena(state: BossState, lane: Float) {
    val lanes = state.level.lanes
    drawRoundRect(BoardFrame, size = size, cornerRadius = CornerRadius(lane * 0.3f))
    for (i in 0 until lanes) {
        val top = i * lane
        val inset = lane * 0.08f
        val targeted = i in state.target
        val color = when {
            targeted && state.phase == BossPhase.CHARGING -> LaserGlow.copy(alpha = 0.28f)
            else -> FloorTop.copy(alpha = 0.55f)
        }
        drawRoundRect(
            color,
            topLeft = Offset(inset, top + inset),
            size = Size(size.width - 2 * inset, lane - 2 * inset),
            cornerRadius = CornerRadius(lane * 0.25f),
        )
        if (targeted && state.phase == BossPhase.CHARGING) {
            // Warning chevrons pointing at Byte.
            val y = top + lane / 2f
            for (k in 0 until 3) {
                val x = size.width * (0.45f + k * 0.1f)
                drawLine(LaserGlow, Offset(x, y - lane * 0.12f), Offset(x - lane * 0.12f, y), strokeWidth = lane * 0.05f, cap = StrokeCap.Round)
                drawLine(LaserGlow, Offset(x - lane * 0.12f, y), Offset(x, y + lane * 0.12f), strokeWidth = lane * 0.05f, cap = StrokeCap.Round)
            }
        }
        if (targeted && state.phase == BossPhase.FIRED) {
            val y = top + lane / 2f
            drawLine(LaserGlow.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width * 0.86f, y), strokeWidth = lane * 0.34f, cap = StrokeCap.Round)
            drawLine(LaserCore, Offset(0f, y), Offset(size.width * 0.86f, y), strokeWidth = lane * 0.12f, cap = StrokeCap.Round)
        }
    }
    drawDrone(Offset(size.width - lane * 0.62f, size.height / 2f), lane * 0.5f, charging = state.phase == BossPhase.CHARGING)
}

private fun DrawScope.drawDrone(centre: Offset, r: Float, charging: Boolean) {
    // Wings
    drawRoundRect(DroneEdge, topLeft = centre + Offset(-r * 0.4f, -r * 1.55f), size = Size(r * 0.8f, r * 3.1f), cornerRadius = CornerRadius(r * 0.4f))
    // Body
    val body = Path().apply {
        for (i in 0 until 6) {
            val angle = Math.toRadians((60.0 * i) + 30.0)
            val point = Offset(centre.x + r * kotlin.math.cos(angle).toFloat(), centre.y + r * kotlin.math.sin(angle).toFloat())
            if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
        close()
    }
    drawPath(body, DroneBody)
    // Eye, brighter while charging
    drawCircle(Color.White, r * 0.48f, centre)
    drawCircle(if (charging) LaserGlow else Color(0xFF3B0F45), r * 0.26f, centre + Offset(-r * 0.08f, 0f))
}
