package au.edu.jcu.codequest.domain

import au.edu.jcu.codequest.domain.flight.Flight
import au.edu.jcu.codequest.domain.flight.FlightCatalog
import au.edu.jcu.codequest.domain.flight.FlightLevel
import au.edu.jcu.codequest.domain.flight.FlightOutcome
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightTest {

    private val open = FlightCatalog.toPlanetTwo

    /** Every angle and power the dials can be set to. */
    private fun dialSettings(): List<Pair<Int, Int>> =
        Flight.ANGLES.step(Flight.ANGLE_STEP).flatMap { angle -> Flight.POWERS.step(Flight.POWER_STEP).map { angle to it } }

    @Test
    fun landingSpot_matchesTheTextbookRangeFormula() {
        // With no obstacles, range = v² · sin(2θ) / g, so 50 at 45° under g = 10 lands at 250.
        val result = Flight.simulate(open, angle = 45, power = 50)

        assertEquals(FlightOutcome.LANDED, result.outcome)
        assertTrue(abs(result.landingX!! - 250.0) < 1.0)
    }

    @Test
    fun tooLittlePower_fallsShort_andTooMuch_overshoots() {
        assertEquals(FlightOutcome.TOO_SHORT, Flight.simulate(open, angle = 45, power = 30).outcome)
        assertEquals(FlightOutcome.TOO_FAR, Flight.simulate(open, angle = 45, power = 80).outcome)
    }

    @Test
    fun complementaryAngles_landInTheSamePlace() {
        val low = Flight.simulate(open, angle = 30, power = 55).landingX!!
        val high = Flight.simulate(open, angle = 60, power = 55).landingX!!

        assertTrue(abs(low - high) < 1.0)
    }

    @Test
    fun hittingAnAsteroid_endsTheFlightAtTheRock() {
        val level = FlightCatalog.toPlanetThree
        val result = Flight.simulate(level, angle = 45, power = 50)

        assertEquals(FlightOutcome.HIT_ASTEROID, result.outcome)
        assertNull(result.landingX)
        val rock = level.asteroids.single()
        val end = result.path.last()
        assertTrue(abs(end.x - rock.x) <= rock.radius && abs(end.y - rock.y) <= rock.radius)
    }

    @Test
    fun pathStartsAtThePadAndEndsOnTheGround() {
        val result = Flight.simulate(open, angle = 40, power = 45)

        assertEquals(0.0, result.path.first().x, 0.0)
        assertEquals(0.0, result.path.last().y, 1e-9)
        assertTrue(result.path.size > 10)
    }

    @Test
    fun everyFlight_canBeLanded_butNotWithItsStartingValues() {
        FlightCatalog.all.forEach { level ->
            val landings = dialSettings().filter { (a, p) -> Flight.simulate(level, a, p).landed }
            assertTrue("${level.title} has no solution", landings.isNotEmpty())
            assertFalse("${level.title} starts solved", Flight.simulate(level, level.startAngle, level.startPower).landed)
        }
    }

    @Test
    fun flights_joinEachPlanetToTheNext() {
        assertEquals(listOf(1, 2, 3), FlightCatalog.all.map { it.fromUnit })
        assertEquals(FlightCatalog.toPlanetThree, FlightCatalog.fromUnit(2))
        assertNull(FlightCatalog.fromUnit(4))
        assertEquals(FlightCatalog.toPlanetFour, FlightCatalog.byId("flight-3"))
    }

    @Test
    fun nudge_staysOnTheDial() {
        assertEquals(15, Flight.nudge(10, Flight.ANGLES, Flight.ANGLE_STEP, +1))
        assertEquals(10, Flight.nudge(10, Flight.ANGLES, Flight.ANGLE_STEP, -1))
        assertEquals(80, Flight.nudge(80, Flight.POWERS, Flight.POWER_STEP, +1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun startValuesOffTheDial_areRejected() {
        FlightLevel("x", 1, "t", "g", "h", gravity = 10.0, distance = 100.0, padRadius = 10.0, startAngle = 7, startPower = 40)
    }
}
