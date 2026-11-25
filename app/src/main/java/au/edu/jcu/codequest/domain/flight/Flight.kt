package au.edu.jcu.codequest.domain.flight

import kotlin.math.cos
import kotlin.math.sin

/** A point in flight space: x to the right, y up, both in the same made-up distance unit. */
data class Point(val x: Double, val y: Double)

/** A rock floating between the planets. Touching it ends the flight. */
data class Asteroid(val x: Double, val y: Double, val radius: Double)

/**
 * One rocket flight from the planet just finished to the next one. The rocket starts at the
 * origin; the next planet's landing pad is centred [distance] away and is [padRadius] wide on
 * each side. Anything that comes down short of the pad or beyond it falls into a wormhole.
 */
data class FlightLevel(
    val id: String,
    /** The unit whose planet the rocket leaves. */
    val fromUnit: Int,
    val title: String,
    val goal: String,
    val hint: String,
    val gravity: Double,
    val distance: Double,
    val padRadius: Double,
    val asteroids: List<Asteroid> = emptyList(),
    val startAngle: Int,
    val startPower: Int,
) {
    /** How far right the flight space reaches; a rocket past this edge is lost. */
    val width: Double get() = distance + padRadius + 60.0

    init {
        require(gravity > 0 && distance > padRadius && padRadius > 0) { "Impossible flight level" }
        require(startAngle in Flight.ANGLES && startPower in Flight.POWERS) { "Start values must be on the dials" }
    }
}

enum class FlightOutcome { LANDED, TOO_SHORT, TOO_FAR, HIT_ASTEROID }

/** The whole path of one launch, sampled at even time steps, ready to animate. */
data class FlightResult(
    val angle: Int,
    val power: Int,
    val path: List<Point>,
    val outcome: FlightOutcome,
    /** Where the rocket came down, or null if it hit an asteroid or left the screen. */
    val landingX: Double?,
) {
    val landed: Boolean get() = outcome == FlightOutcome.LANDED
}

/**
 * The physics of a launch: `launch(angle, power)` throws the rocket along a parabola,
 * x = v·cos(θ)·t and y = v·sin(θ)·t − g·t²/2, so the learner sees how two parameters shape
 * one function's result.
 */
object Flight {
    val ANGLES: IntRange = 10..80
    val POWERS: IntRange = 20..80
    const val ANGLE_STEP = 5
    const val POWER_STEP = 5

    private const val TIME_STEP = 0.02
    private const val MAX_TIME = 60.0

    /** Keeps only every n-th sample in the result, which is plenty for drawing and animating. */
    private const val KEEP_EVERY = 3

    fun simulate(level: FlightLevel, angle: Int, power: Int): FlightResult {
        val theta = Math.toRadians(angle.toDouble())
        val vx = power * cos(theta)
        val vy = power * sin(theta)
        val path = mutableListOf(Point(0.0, 0.0))
        var previous = Point(0.0, 0.0)
        var step = 0
        while (step * TIME_STEP < MAX_TIME) {
            step++
            val t = step * TIME_STEP
            val point = Point(vx * t, vy * t - level.gravity * t * t / 2)
            if (level.asteroids.any { it.contains(point) }) {
                path += point
                return FlightResult(angle, power, path, FlightOutcome.HIT_ASTEROID, landingX = null)
            }
            if (point.y < 0) {
                // Where the path crossed y = 0 between the last two samples.
                val landingX = previous.x + (point.x - previous.x) * (previous.y / (previous.y - point.y))
                path += Point(landingX, 0.0)
                val outcome = when {
                    landingX < level.distance - level.padRadius -> FlightOutcome.TOO_SHORT
                    landingX > level.distance + level.padRadius -> FlightOutcome.TOO_FAR
                    else -> FlightOutcome.LANDED
                }
                return FlightResult(angle, power, path, outcome, landingX)
            }
            if (point.x > level.width) {
                path += point
                return FlightResult(angle, power, path, FlightOutcome.TOO_FAR, landingX = null)
            }
            if (step % KEEP_EVERY == 0) path += point
            previous = point
        }
        return FlightResult(angle, power, path, FlightOutcome.TOO_FAR, landingX = null)
    }

    /** Moves a dial value one step, staying on the dial. */
    fun nudge(value: Int, range: IntRange, step: Int, direction: Int): Int =
        (value + direction * step).coerceIn(range.first, range.last)

    private fun Asteroid.contains(point: Point): Boolean {
        val dx = point.x - x
        val dy = point.y - y
        return dx * dx + dy * dy < radius * radius
    }
}

/** The three flights that join the four planets, one after each unit's mission. */
object FlightCatalog {

    val toPlanetTwo = FlightLevel(
        id = "flight-1",
        fromUnit = 1,
        title = "Hop to planet 2",
        goal = "Land on the next planet's pad.",
        hint = "Power changes how far the rocket goes. An angle of 45 goes furthest for the same power.",
        gravity = 10.0,
        distance = 250.0,
        padRadius = 25.0,
        startAngle = 30,
        startPower = 40,
    )

    val toPlanetThree = FlightLevel(
        id = "flight-2",
        fromUnit = 2,
        title = "Around the rock",
        goal = "Reach planet 3 without touching the asteroid.",
        hint = "Fly low and fast under the asteroid, or high and slow over it. Both can work.",
        gravity = 6.0,
        distance = 300.0,
        padRadius = 22.0,
        asteroids = listOf(Asteroid(150.0, 95.0, 28.0)),
        startAngle = 45,
        startPower = 40,
    )

    val toPlanetFour = FlightLevel(
        id = "flight-3",
        fromUnit = 3,
        title = "Asteroid alley",
        goal = "Thread the gap between two asteroids and land on planet 4.",
        hint = "Change one value at a time and watch where the last flight went. A steep, strong launch can clear both rocks.",
        gravity = 9.0,
        distance = 280.0,
        padRadius = 22.0,
        asteroids = listOf(Asteroid(120.0, 60.0, 20.0), Asteroid(210.0, 150.0, 24.0)),
        startAngle = 40,
        startPower = 40,
    )

    val all: List<FlightLevel> = listOf(toPlanetTwo, toPlanetThree, toPlanetFour)

    fun byId(id: String): FlightLevel? = all.firstOrNull { it.id == id }

    /** The flight that leaves the planet of [unitIndex], if there is one. */
    fun fromUnit(unitIndex: Int): FlightLevel? = all.firstOrNull { it.fromUnit == unitIndex }
}
