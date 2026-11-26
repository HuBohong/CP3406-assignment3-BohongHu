package au.edu.jcu.codequest.ui.mission

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import au.edu.jcu.codequest.R
import au.edu.jcu.codequest.domain.mission.BossState
import au.edu.jcu.codequest.domain.mission.Cell
import au.edu.jcu.codequest.domain.mission.GridState
import au.edu.jcu.codequest.domain.mission.Heading
import au.edu.jcu.codequest.domain.mission.Tile

// What TalkBack reads for the mission boards.

/** "Row 2, column 3: rock" for every square of the free-build planet, row by row. */
@Composable
internal fun sandboxCellLabels(state: GridState): List<String> {
    val context = LocalContext.current
    val names = mapOf(
        "byte" to stringResource(R.string.sandbox_tool_start),
        "crystal" to stringResource(R.string.sandbox_tool_crystal),
        "rock" to stringResource(R.string.sandbox_tool_rock),
        "crater" to stringResource(R.string.sandbox_tool_crater),
        "goal" to stringResource(R.string.sandbox_tool_goal),
        "floor" to stringResource(R.string.sandbox_tool_floor),
    )
    val level = state.level
    return (0 until level.height).flatMap { row ->
        (0 until level.width).map { column ->
            val cell = Cell(column, row)
            val what = when {
                cell == state.byte -> names.getValue("byte")
                cell in state.crystalsLeft -> names.getValue("crystal")
                else -> when (level.tileAt(cell)) {
                    Tile.ROCK -> names.getValue("rock")
                    Tile.CRATER -> names.getValue("crater")
                    Tile.GOAL -> names.getValue("goal")
                    Tile.FLOOR, null -> names.getValue("floor")
                }
            }
            context.getString(R.string.sandbox_cell, row + 1, column + 1, what)
        }
    }
}

@Composable
internal fun gridDescription(state: GridState): String = stringResource(
    R.string.mission_grid_description,
    state.byte.x + 1,
    state.byte.y + 1,
    stringResource(
        when (state.heading) {
            Heading.NORTH -> R.string.heading_up
            Heading.EAST -> R.string.heading_right
            Heading.SOUTH -> R.string.heading_down
            Heading.WEST -> R.string.heading_left
        },
    ),
    state.collected,
    state.level.crystals.size,
)

@Composable
internal fun bossDescription(state: BossState): String = stringResource(
    R.string.mission_boss_description,
    state.byteLane + 1,
    state.turn + 1,
    state.totalTurns,
    state.target.sorted().joinToString { (it + 1).toString() },
    state.shields,
)
