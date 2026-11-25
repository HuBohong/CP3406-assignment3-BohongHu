package au.edu.jcu.codequest.domain.mission

/** What a tap on the free-build board puts on the tapped square. */
enum class BuildTool(val symbol: Char) {
    FLOOR('.'),
    CRYSTAL('C'),
    ROCK('#'),
    CRATER('o'),
    GOAL('G'),
    START('B'),
}

/**
 * A planet the learner designs in free-build mode, stored as the same text rows the built-in
 * missions use. There is always exactly one start and one beacon: placing either moves it,
 * and the squares they stand on cannot be painted over.
 */
data class SandboxMap(val rows: List<String>, val heading: Heading = Heading.EAST) {

    init {
        require(rows.size == SIZE && rows.all { it.length == SIZE }) { "A sandbox map is $SIZE x $SIZE" }
        require(rows.sumOf { row -> row.count { it == 'B' } } == 1) { "Exactly one start" }
        require(rows.sumOf { row -> row.count { it == 'G' } } == 1) { "Exactly one beacon" }
        require(rows.all { row -> row.all { it in ".#oCGB" } }) { "Unknown tile" }
    }

    fun symbolAt(cell: Cell): Char? = rows.getOrNull(cell.y)?.getOrNull(cell.x)

    /** Returns the map with [tool] used on [cell]; unchanged when the tap is not allowed. */
    fun paint(cell: Cell, tool: BuildTool): SandboxMap {
        val current = symbolAt(cell) ?: return this
        return when (tool) {
            // Tapping the start again turns Byte, so the learner can choose where it faces.
            BuildTool.START -> if (current == 'B') copy(heading = heading.turnedRight()) else move('B', cell)
            BuildTool.GOAL -> if (current == 'G') this else move('G', cell)
            else -> if (current == 'B' || current == 'G') this else set(cell, tool.symbol)
        }
    }

    fun toLevel(): GridLevel = GridLevel.parse(rows, heading)

    /** A compact form for saving, e.g. "E|B.....|......|...". */
    fun encode(): String = heading.name.first() + "|" + rows.joinToString("|")

    /** Moves the start or beacon to [to]; refused when the other one stands there. */
    private fun move(symbol: Char, to: Cell): SandboxMap {
        if (symbolAt(to) == 'B' || symbolAt(to) == 'G') return this
        return copy(rows = rows.map { it.replace(symbol, '.') }.withSymbol(to, symbol))
    }

    private fun set(cell: Cell, symbol: Char): SandboxMap = copy(rows = rows.withSymbol(cell, symbol))

    private fun List<String>.withSymbol(cell: Cell, symbol: Char): List<String> = mapIndexed { y, row ->
        if (y != cell.y) row else row.substring(0, cell.x) + symbol + row.substring(cell.x + 1)
    }

    companion object {
        const val SIZE = 6

        val DEFAULT = SandboxMap(
            listOf(
                "B.....",
                "......",
                "..C...",
                "......",
                "....#.",
                ".....G",
            ),
        )

        /** Reads a saved map, or returns null if the text is damaged or from an older format. */
        fun decode(text: String?): SandboxMap? {
            if (text.isNullOrBlank()) return null
            val parts = text.split('|')
            val heading = Heading.entries.firstOrNull { it.name.first() == parts.first().firstOrNull() } ?: return null
            return runCatching { SandboxMap(parts.drop(1), heading) }.getOrNull()
        }
    }
}

/** The free-build planet: the learner's own map and every grid block they have collected. */
object Sandbox {
    const val ID = "sandbox"

    /** Blocks that mean something on a grid planet, in toolbox order. */
    val gridBlocks: List<PaletteBlock> = BlockCollection.all.filter { block ->
        block !is PaletteBlock.Act || block.action in setOf(Action.MOVE, Action.TURN_LEFT, Action.TURN_RIGHT, Action.COLLECT)
    }

    fun mission(map: SandboxMap): Mission = Mission(
        id = ID,
        unitIndex = 0,
        kind = MissionKind.GRID,
        title = "Free-build planet",
        goal = "Design a planet, then program Byte to collect every crystal and reach the beacon.",
        hint = "Switch to Build to change the planet. Tap Byte's square again with the start tool to turn it.",
        palette = gridBlocks,
        sensors = listOf(Sensor.PATH_AHEAD, Sensor.ON_CRYSTAL, Sensor.AT_GOAL),
        referenceSolution = Program(),
        grid = map.toLevel(),
    )
}
