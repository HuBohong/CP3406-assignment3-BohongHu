package au.edu.jcu.codequest.domain.mission

/** What a mission board looks like at one moment. Implemented by each kind of world. */
sealed interface MissionState

/** Something worth showing or announcing as the program runs. */
enum class MissionEvent {
    MOVED,
    TURNED,
    COLLECTED,
    NOTHING_TO_COLLECT,
    BUMPED,
    FELL,
    LEFT_MAP,
    CHARGING,
    DODGED,
    HIT,
    BLOCKED_AT_EDGE,
    STAYED,
}

/** Why a run did not succeed, so the screen can explain what to change. */
enum class FailureReason {
    EMPTY_PROGRAM,
    BUMPED_INTO_ROCK,
    LEFT_THE_MAP,
    FELL_INTO_CRATER,
    CRYSTALS_LEFT,
    NOT_AT_GOAL,
    TOO_MANY_STEPS,
    SHIELDS_GONE,
}

sealed interface MissionOutcome {
    data object Success : MissionOutcome
    data class Failure(val reason: FailureReason) : MissionOutcome
}

/**
 * One step of the replay: the board after the step, which block was running (null for steps
 * the world takes itself, such as the boss firing) and what happened.
 */
data class Frame(
    val state: MissionState,
    val activeBlockId: Int?,
    val event: MissionEvent?,
)

/** A finished run: every frame to animate, in order, and how it ended. */
data class MissionRun(
    val frames: List<Frame>,
    val outcome: MissionOutcome,
)

/** Result of asking the world to perform one action. A non-null [failure] ends the run. */
data class ActionResult(
    val event: MissionEvent?,
    val failure: FailureReason? = null,
)

/** A board a program can act on. */
interface MissionWorld {
    val state: MissionState
    fun sense(sensor: Sensor): Boolean
    fun perform(action: Action): ActionResult
}

/**
 * Runs learners' block programs against a [MissionWorld] and records a [Frame] for every
 * action and every condition check, so the screen can replay the run step by step with the
 * running block highlighted.
 *
 * Every action and condition check costs one step. A program that runs out of steps (most
 * often a `while` loop whose condition never changes) stops with [FailureReason.TOO_MANY_STEPS]
 * instead of hanging the app.
 */
class Interpreter(
    private val world: MissionWorld,
    private val frames: MutableList<Frame>,
    private val maxSteps: Int,
    private val endTurnOnMovement: Boolean,
) {
    /** How a block, or a list of blocks, finished. */
    sealed interface Signal {
        data object Continue : Signal
        data object TurnEnded : Signal
        data class Stop(val reason: FailureReason) : Signal
    }

    private var steps = 0

    fun run(blocks: List<Block>): Signal {
        for (block in blocks) {
            val signal = execute(block)
            if (signal != Signal.Continue) return signal
        }
        return Signal.Continue
    }

    private fun execute(block: Block): Signal = when (block) {
        is CommandBlock -> {
            if (!tick()) {
                Signal.Stop(FailureReason.TOO_MANY_STEPS)
            } else {
                val result = world.perform(block.action)
                frames += Frame(world.state, block.id, result.event)
                when {
                    result.failure != null -> Signal.Stop(result.failure)
                    endTurnOnMovement && block.action.isMovement -> Signal.TurnEnded
                    else -> Signal.Continue
                }
            }
        }
        is RepeatBlock -> repeatBody(block)
        is IfBlock -> {
            if (!tick()) {
                Signal.Stop(FailureReason.TOO_MANY_STEPS)
            } else {
                val holds = check(block.condition)
                frames += Frame(world.state, block.id, null)
                run(if (holds) block.body else block.elseBody.orEmpty())
            }
        }
        is WhileBlock -> whileLoop(block)
    }

    private fun repeatBody(block: RepeatBlock): Signal {
        repeat(block.times) {
            if (!tick()) return Signal.Stop(FailureReason.TOO_MANY_STEPS)
            frames += Frame(world.state, block.id, null)
            val signal = run(block.body)
            if (signal != Signal.Continue) return signal
        }
        return Signal.Continue
    }

    private fun whileLoop(block: WhileBlock): Signal {
        while (true) {
            if (!tick()) return Signal.Stop(FailureReason.TOO_MANY_STEPS)
            val holds = check(block.condition)
            frames += Frame(world.state, block.id, null)
            if (!holds) return Signal.Continue
            val signal = run(block.body)
            if (signal != Signal.Continue) return signal
        }
    }

    private fun check(condition: Condition): Boolean = world.sense(condition.sensor) != condition.negated

    private fun tick(): Boolean {
        steps++
        return steps <= maxSteps
    }
}
