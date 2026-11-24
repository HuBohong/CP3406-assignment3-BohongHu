package au.edu.jcu.codequest.domain.mission

enum class MissionKind { GRID, BOSS }

/**
 * The end-of-unit challenge that replaces a test: the learner builds a program from the blocks
 * in [palette] and watches Byte run it.
 *
 * [referenceSolution] is one program that solves the mission. It is never shown as the answer;
 * unit tests use it to prove every mission can be solved with its own palette.
 */
data class Mission(
    val id: String,
    val unitIndex: Int,
    val kind: MissionKind,
    val title: String,
    val goal: String,
    val hint: String,
    val palette: List<PaletteBlock>,
    val sensors: List<Sensor>,
    val referenceSolution: Program,
    val grid: GridLevel? = null,
    val boss: BossLevel? = null,
) {
    init {
        require((kind == MissionKind.GRID) == (grid != null) && (kind == MissionKind.BOSS) == (boss != null)) {
            "A $kind mission needs exactly its own kind of level"
        }
    }

    /** The board before anything runs, for the screen to draw while the learner builds. */
    val initialState: MissionState
        get() = when (kind) {
            MissionKind.GRID -> GridState.start(requireNotNull(grid))
            MissionKind.BOSS -> BossWorld(requireNotNull(boss)).state
        }
}

/** Runs a program on a mission and records the whole run, ready to replay. */
object MissionRunner {
    private const val GRID_MAX_STEPS = 400
    private const val BOSS_MAX_STEPS_PER_TURN = 60

    fun run(mission: Mission, program: Program): MissionRun {
        if (program.blocks.isEmpty()) {
            return MissionRun(listOf(Frame(mission.initialState, null, null)), MissionOutcome.Failure(FailureReason.EMPTY_PROGRAM))
        }
        return when (mission.kind) {
            MissionKind.GRID -> runGrid(requireNotNull(mission.grid), program)
            MissionKind.BOSS -> runBoss(requireNotNull(mission.boss), program)
        }
    }

    private fun runGrid(level: GridLevel, program: Program): MissionRun {
        val world = GridWorld(level)
        val frames = mutableListOf(Frame(world.state, null, null))
        val signal = Interpreter(world, frames, GRID_MAX_STEPS, endTurnOnMovement = false).run(program.blocks)
        val outcome = if (signal is Interpreter.Signal.Stop) {
            MissionOutcome.Failure(signal.reason)
        } else {
            world.finalOutcome()
        }
        return MissionRun(frames, outcome)
    }

    private fun runBoss(level: BossLevel, program: Program): MissionRun {
        val world = BossWorld(level)
        val frames = mutableListOf<Frame>()
        for (turn in level.attacks.indices) {
            world.beginTurn(turn)
            frames += Frame(world.state, null, MissionEvent.CHARGING)
            val signal = Interpreter(world, frames, BOSS_MAX_STEPS_PER_TURN, endTurnOnMovement = true).run(program.blocks)
            if (signal is Interpreter.Signal.Stop) return MissionRun(frames, MissionOutcome.Failure(signal.reason))
            val hit = world.fire()
            frames += Frame(world.state, null, if (hit) MissionEvent.HIT else MissionEvent.DODGED)
            if (world.state.shields <= 0) return MissionRun(frames, MissionOutcome.Failure(FailureReason.SHIELDS_GONE))
        }
        return MissionRun(frames, MissionOutcome.Success)
    }
}

/** The four end-of-unit missions, one per unit, in path order. */
object MissionCatalog {

    private val move = PaletteBlock.Act(Action.MOVE)
    private val turnLeft = PaletteBlock.Act(Action.TURN_LEFT)
    private val turnRight = PaletteBlock.Act(Action.TURN_RIGHT)
    private val collect = PaletteBlock.Act(Action.COLLECT)

    val landingSite = Mission(
        id = "mission-1",
        unitIndex = 1,
        kind = MissionKind.GRID,
        title = "Landing site",
        goal = "Pick up both crystals, then reach the beacon.",
        hint = "Byte runs your blocks from top to bottom, one at a time. Count the squares before you turn.",
        palette = listOf(move, turnLeft, turnRight, collect),
        sensors = emptyList(),
        grid = GridLevel.parse(
            listOf(
                "B..C.",
                "##o#.",
                "o#.oC",
                "..#.G",
            ),
        ),
        referenceSolution = Program.of(
            CommandBlock(1, Action.MOVE),
            CommandBlock(2, Action.MOVE),
            CommandBlock(3, Action.MOVE),
            CommandBlock(4, Action.COLLECT),
            CommandBlock(5, Action.MOVE),
            CommandBlock(6, Action.TURN_RIGHT),
            CommandBlock(7, Action.MOVE),
            CommandBlock(8, Action.MOVE),
            CommandBlock(9, Action.COLLECT),
            CommandBlock(10, Action.MOVE),
        ),
    )

    val crystalCanyon = Mission(
        id = "mission-2",
        unitIndex = 2,
        kind = MissionKind.GRID,
        title = "Crystal canyon",
        goal = "Collect every crystal on the way to the beacon.",
        hint = "Writing move() six times works, but a repeat loop is shorter. Use if (onCrystal()) so Byte only collects where there is a crystal.",
        palette = listOf(move, collect, PaletteBlock.Repeat, PaletteBlock.If),
        sensors = listOf(Sensor.ON_CRYSTAL, Sensor.PATH_AHEAD),
        grid = GridLevel.parse(
            listOf(
                "o#o#oo#",
                "B.C.CCG",
                "#o#o##o",
            ),
        ),
        referenceSolution = Program.of(
            RepeatBlock(
                1,
                times = 6,
                body = listOf(
                    CommandBlock(2, Action.MOVE),
                    IfBlock(3, Condition(Sensor.ON_CRYSTAL), body = listOf(CommandBlock(4, Action.COLLECT))),
                ),
            ),
        ),
    )

    val laserDrone = Mission(
        id = "mission-3",
        unitIndex = 3,
        kind = MissionKind.BOSS,
        title = "Laser drone",
        goal = "Survive all 8 attacks. Your program runs once every turn.",
        hint = "Only move when the laser is aimed at you. At the top lane you can't go up, so check safeAbove() and use else.",
        palette = listOf(
            PaletteBlock.Act(Action.MOVE_UP),
            PaletteBlock.Act(Action.MOVE_DOWN),
            PaletteBlock.Act(Action.STAY),
            PaletteBlock.If,
            PaletteBlock.IfElse,
        ),
        sensors = listOf(Sensor.LASER_HERE, Sensor.SAFE_ABOVE, Sensor.SAFE_BELOW),
        boss = BossLevel(
            attacks = listOf(
                setOf(1),
                setOf(0),
                setOf(2),
                setOf(1),
                setOf(0, 1),
                setOf(2),
                setOf(1, 2),
                setOf(0),
            ),
        ),
        referenceSolution = Program.of(
            IfBlock(
                1,
                Condition(Sensor.LASER_HERE),
                body = listOf(
                    IfBlock(
                        2,
                        Condition(Sensor.SAFE_ABOVE),
                        body = listOf(CommandBlock(3, Action.MOVE_UP)),
                        elseBody = listOf(CommandBlock(4, Action.MOVE_DOWN)),
                    ),
                ),
            ),
        ),
    )

    val longWayHome = Mission(
        id = "mission-4",
        unitIndex = 4,
        kind = MissionKind.GRID,
        title = "The long way home",
        goal = "Reach the beacon at the end of the winding canyon.",
        hint = "You don't need to count squares. Keep going while you are not at the goal: move if the path ahead is clear, otherwise turn.",
        palette = listOf(move, turnLeft, turnRight, PaletteBlock.IfElse, PaletteBlock.While),
        sensors = listOf(Sensor.AT_GOAL, Sensor.PATH_AHEAD),
        grid = GridLevel.parse(
            listOf(
                "B....",
                "##.o.",
                "..G#.",
                ".#o#.",
                ".....",
            ),
        ),
        referenceSolution = Program.of(
            WhileBlock(
                1,
                Condition(Sensor.AT_GOAL, negated = true),
                body = listOf(
                    IfBlock(
                        2,
                        Condition(Sensor.PATH_AHEAD),
                        body = listOf(CommandBlock(3, Action.MOVE)),
                        elseBody = listOf(CommandBlock(4, Action.TURN_RIGHT)),
                    ),
                ),
            ),
        ),
    )

    val all: List<Mission> = listOf(landingSite, crystalCanyon, laserDrone, longWayHome)

    fun byId(id: String): Mission? = all.firstOrNull { it.id == id }

    fun forUnit(unitIndex: Int): Mission? = all.firstOrNull { it.unitIndex == unitIndex }
}
