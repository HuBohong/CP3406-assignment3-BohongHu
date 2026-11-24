package au.edu.jcu.codequest.domain.mission

/**
 * A boss fight: the boss attacks one or two of [lanes] each turn and Byte must not be in an
 * attacked lane when it fires. Lane 0 is the top.
 */
data class BossLevel(
    val lanes: Int = 3,
    val startLane: Int = 1,
    val shields: Int = 3,
    val attacks: List<Set<Int>>,
) {
    init {
        require(attacks.all { attack -> attack.isNotEmpty() && attack.size < lanes && attack.all { it in 0 until lanes } }) {
            "Every attack must leave at least one lane safe"
        }
    }
}

enum class BossPhase { CHARGING, FIRED }

data class BossState(
    val level: BossLevel,
    val byteLane: Int,
    val shields: Int,
    /** 0-based number of the current attack. */
    val turn: Int,
    /** Lanes the boss is aiming at this turn. */
    val target: Set<Int>,
    val phase: BossPhase,
    val hitThisTurn: Boolean = false,
) : MissionState {
    val totalTurns: Int get() = level.attacks.size
}

/**
 * Byte's side of the fight. Each turn Byte runs the learner's program once, and the first
 * movement (`moveUp()`, `moveDown()` or `stay()`) ends its turn. Sensors look at the attack
 * that is charging, so the program can react before the boss fires.
 */
class BossWorld(private val level: BossLevel) : MissionWorld {

    override var state: BossState = BossState(
        level = level,
        byteLane = level.startLane,
        shields = level.shields,
        turn = 0,
        target = level.attacks.first(),
        phase = BossPhase.CHARGING,
    )
        private set

    fun beginTurn(turn: Int) {
        state = state.copy(turn = turn, target = level.attacks[turn], phase = BossPhase.CHARGING, hitThisTurn = false)
    }

    /** The boss fires at its target lanes. Returns true if Byte was hit. */
    fun fire(): Boolean {
        val hit = state.byteLane in state.target
        state = state.copy(
            phase = BossPhase.FIRED,
            hitThisTurn = hit,
            shields = if (hit) state.shields - 1 else state.shields,
        )
        return hit
    }

    override fun sense(sensor: Sensor): Boolean = when (sensor) {
        Sensor.LASER_HERE -> state.byteLane in state.target
        Sensor.SAFE_ABOVE -> state.byteLane > 0 && (state.byteLane - 1) !in state.target
        Sensor.SAFE_BELOW -> state.byteLane < level.lanes - 1 && (state.byteLane + 1) !in state.target
        // Grid sensors mean nothing in the arena.
        Sensor.PATH_AHEAD, Sensor.ON_CRYSTAL, Sensor.AT_GOAL -> false
    }

    override fun perform(action: Action): ActionResult = when (action) {
        Action.MOVE_UP -> if (state.byteLane > 0) {
            state = state.copy(byteLane = state.byteLane - 1)
            ActionResult(MissionEvent.MOVED)
        } else {
            ActionResult(MissionEvent.BLOCKED_AT_EDGE)
        }
        Action.MOVE_DOWN -> if (state.byteLane < level.lanes - 1) {
            state = state.copy(byteLane = state.byteLane + 1)
            ActionResult(MissionEvent.MOVED)
        } else {
            ActionResult(MissionEvent.BLOCKED_AT_EDGE)
        }
        Action.STAY -> ActionResult(MissionEvent.STAYED)
        // Grid actions do nothing in the arena.
        Action.MOVE, Action.TURN_LEFT, Action.TURN_RIGHT, Action.COLLECT -> ActionResult(null)
    }
}
