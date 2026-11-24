package au.edu.jcu.codequest.domain.mission

enum class Tile { FLOOR, ROCK, CRATER, GOAL }

enum class Heading(val dx: Int, val dy: Int) {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0),
    ;

    fun turnedLeft(): Heading = entries[(ordinal + 3) % 4]
    fun turnedRight(): Heading = entries[(ordinal + 1) % 4]
}

data class Cell(val x: Int, val y: Int) {
    fun step(heading: Heading) = Cell(x + heading.dx, y + heading.dy)
}

/** The starting layout of a grid mission. */
data class GridLevel(
    val width: Int,
    val height: Int,
    val tiles: List<List<Tile>>,
    val crystals: Set<Cell>,
    val start: Cell,
    val heading: Heading,
) {
    fun tileAt(cell: Cell): Tile? = tiles.getOrNull(cell.y)?.getOrNull(cell.x)

    companion object {
        /**
         * Builds a level from text rows: `.` floor, `#` rock, `o` crater, `C` crystal on floor,
         * `G` goal and `B` Byte's start (on floor). All rows must be the same length.
         */
        fun parse(rows: List<String>, heading: Heading = Heading.EAST): GridLevel {
            require(rows.isNotEmpty() && rows.all { it.length == rows[0].length }) { "Rows must be non-empty and equal length" }
            var start: Cell? = null
            val crystals = mutableSetOf<Cell>()
            val tiles = rows.mapIndexed { y, row ->
                row.mapIndexed { x, symbol ->
                    when (symbol) {
                        '.' -> Tile.FLOOR
                        '#' -> Tile.ROCK
                        'o' -> Tile.CRATER
                        'G' -> Tile.GOAL
                        'C' -> Tile.FLOOR.also { crystals += Cell(x, y) }
                        'B' -> Tile.FLOOR.also { start = Cell(x, y) }
                        else -> throw IllegalArgumentException("Unknown tile '$symbol'")
                    }
                }
            }
            return GridLevel(
                width = rows[0].length,
                height = rows.size,
                tiles = tiles,
                crystals = crystals,
                start = requireNotNull(start) { "Level needs a start 'B'" },
                heading = heading,
            )
        }
    }
}

/** Byte on a grid planet: where it is, which way it faces and which crystals are left. */
data class GridState(
    val level: GridLevel,
    val byte: Cell,
    val heading: Heading,
    val crystalsLeft: Set<Cell>,
    val trail: List<Cell>,
) : MissionState {
    val collected: Int get() = level.crystals.size - crystalsLeft.size
    val atGoal: Boolean get() = level.tileAt(byte) == Tile.GOAL

    companion object {
        fun start(level: GridLevel) = GridState(level, level.start, level.heading, level.crystals, listOf(level.start))
    }
}

class GridWorld(level: GridLevel) : MissionWorld {

    override var state: GridState = GridState.start(level)
        private set

    override fun sense(sensor: Sensor): Boolean = when (sensor) {
        Sensor.PATH_AHEAD -> state.level.tileAt(state.byte.step(state.heading)).let { it == Tile.FLOOR || it == Tile.GOAL }
        Sensor.ON_CRYSTAL -> state.byte in state.crystalsLeft
        Sensor.AT_GOAL -> state.atGoal
        // Boss sensors mean nothing on a grid.
        Sensor.LASER_HERE, Sensor.SAFE_ABOVE, Sensor.SAFE_BELOW -> false
    }

    override fun perform(action: Action): ActionResult = when (action) {
        Action.MOVE -> move()
        Action.TURN_LEFT -> {
            state = state.copy(heading = state.heading.turnedLeft())
            ActionResult(MissionEvent.TURNED)
        }
        Action.TURN_RIGHT -> {
            state = state.copy(heading = state.heading.turnedRight())
            ActionResult(MissionEvent.TURNED)
        }
        Action.COLLECT -> if (state.byte in state.crystalsLeft) {
            state = state.copy(crystalsLeft = state.crystalsLeft - state.byte)
            ActionResult(MissionEvent.COLLECTED)
        } else {
            ActionResult(MissionEvent.NOTHING_TO_COLLECT)
        }
        // Lane moves belong to the boss arena; on a grid Byte just waits.
        Action.MOVE_UP, Action.MOVE_DOWN, Action.STAY -> ActionResult(MissionEvent.STAYED)
    }

    private fun move(): ActionResult {
        val next = state.byte.step(state.heading)
        return when (state.level.tileAt(next)) {
            null -> ActionResult(MissionEvent.LEFT_MAP, FailureReason.LEFT_THE_MAP)
            Tile.ROCK -> ActionResult(MissionEvent.BUMPED, FailureReason.BUMPED_INTO_ROCK)
            Tile.CRATER -> {
                state = state.copy(byte = next, trail = state.trail + next)
                ActionResult(MissionEvent.FELL, FailureReason.FELL_INTO_CRATER)
            }
            Tile.FLOOR, Tile.GOAL -> {
                state = state.copy(byte = next, trail = state.trail + next)
                ActionResult(MissionEvent.MOVED)
            }
        }
    }

    /** Checked after the program ends: every crystal collected and Byte on the goal. */
    fun finalOutcome(): MissionOutcome = when {
        state.crystalsLeft.isNotEmpty() -> MissionOutcome.Failure(FailureReason.CRYSTALS_LEFT)
        !state.atGoal -> MissionOutcome.Failure(FailureReason.NOT_AT_GOAL)
        else -> MissionOutcome.Success
    }
}
