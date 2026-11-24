package au.edu.jcu.codequest.domain.mission

/** Something Byte can do. [code] is how the block reads in the program. */
enum class Action(val code: String, val isMovement: Boolean) {
    MOVE("move()", isMovement = true),
    TURN_LEFT("turnLeft()", isMovement = false),
    TURN_RIGHT("turnRight()", isMovement = false),
    COLLECT("collect()", isMovement = false),
    MOVE_UP("moveUp()", isMovement = true),
    MOVE_DOWN("moveDown()", isMovement = true),
    STAY("stay()", isMovement = true),
}

/** Something Byte can check. Every sensor answers true or false. */
enum class Sensor(val code: String) {
    PATH_AHEAD("pathAhead()"),
    ON_CRYSTAL("onCrystal()"),
    AT_GOAL("atGoal()"),
    LASER_HERE("laserHere()"),
    SAFE_ABOVE("safeAbove()"),
    SAFE_BELOW("safeBelow()"),
}

/** A sensor reading, optionally negated with `!`. */
data class Condition(val sensor: Sensor, val negated: Boolean = false) {
    val code: String get() = (if (negated) "!" else "") + sensor.code
}

/** One block of a learner's program. Every block has an id that is unique within its program. */
sealed interface Block {
    val id: Int
}

data class CommandBlock(override val id: Int, val action: Action) : Block

data class RepeatBlock(
    override val id: Int,
    val times: Int,
    val body: List<Block> = emptyList(),
) : Block

/** `if (condition) { body }`, plus `else { elseBody }` when [elseBody] is not null. */
data class IfBlock(
    override val id: Int,
    val condition: Condition,
    val body: List<Block> = emptyList(),
    val elseBody: List<Block>? = null,
) : Block

data class WhileBlock(
    override val id: Int,
    val condition: Condition,
    val body: List<Block> = emptyList(),
) : Block

/** The kinds of block a learner can pick from the toolbox. */
sealed interface PaletteBlock {
    val label: String

    data class Act(val action: Action) : PaletteBlock {
        override val label: String get() = action.code
    }

    data object Repeat : PaletteBlock {
        override val label = "repeat(n) { }"
    }

    data object If : PaletteBlock {
        override val label = "if (…) { }"
    }

    data object IfElse : PaletteBlock {
        override val label = "if … else"
    }

    data object While : PaletteBlock {
        override val label = "while (…) { }"
    }
}

/** Where a new block goes: the top level, or inside a block's body or else-branch. */
sealed interface Slot {
    data object Root : Slot
    data class Body(val blockId: Int) : Slot
    data class Else(val blockId: Int) : Slot
}

/**
 * A learner's program, as an immutable tree. Every edit returns a new [Program], which keeps
 * ViewModel state simple and makes the editing rules easy to unit test.
 */
data class Program(
    val blocks: List<Block> = emptyList(),
    val nextId: Int = 1,
) {
    /** Number of blocks, counting those nested inside others. */
    val size: Int get() = countBlocks(blocks)

    /** Id the next inserted block will get; after [insert] the new block has `nextId - 1`. */
    val lastInsertedId: Int get() = nextId - 1

    /**
     * Appends a new block built from [item] at the end of [slot]. Conditions start with
     * [defaultSensor]. Inserting into a slot that does not exist changes nothing.
     */
    fun insert(item: PaletteBlock, slot: Slot, defaultSensor: Sensor): Program {
        val block = when (item) {
            is PaletteBlock.Act -> CommandBlock(nextId, item.action)
            PaletteBlock.Repeat -> RepeatBlock(nextId, times = DEFAULT_REPEAT)
            PaletteBlock.If -> IfBlock(nextId, Condition(defaultSensor))
            PaletteBlock.IfElse -> IfBlock(nextId, Condition(defaultSensor), elseBody = emptyList())
            PaletteBlock.While -> WhileBlock(nextId, Condition(defaultSensor))
        }
        if (slot != Slot.Root && !hasSlot(slot)) return this
        val updated = when (slot) {
            Slot.Root -> blocks + block
            is Slot.Body -> update(blocks, slot.blockId) { it.withBody(it.bodyOrEmpty() + block) }
            is Slot.Else -> update(blocks, slot.blockId) { target ->
                if (target is IfBlock && target.elseBody != null) target.copy(elseBody = target.elseBody + block) else target
            }
        }
        return Program(updated, nextId + 1)
    }

    /** Removes a block and everything inside it. */
    fun remove(blockId: Int): Program = copy(blocks = removeFrom(blocks, blockId))

    fun setRepeatTimes(blockId: Int, times: Int): Program = copy(
        blocks = update(blocks, blockId) {
            if (it is RepeatBlock) it.copy(times = times.coerceIn(MIN_REPEAT, MAX_REPEAT)) else it
        },
    )

    fun setCondition(blockId: Int, condition: Condition): Program = copy(
        blocks = update(blocks, blockId) {
            when (it) {
                is IfBlock -> it.copy(condition = condition)
                is WhileBlock -> it.copy(condition = condition)
                else -> it
            }
        },
    )

    fun find(blockId: Int): Block? = findIn(blocks, blockId)

    /** True when [slot] can receive blocks in this program. */
    fun hasSlot(slot: Slot): Boolean = when (slot) {
        Slot.Root -> true
        is Slot.Body -> find(slot.blockId).let { it != null && it !is CommandBlock }
        is Slot.Else -> (find(slot.blockId) as? IfBlock)?.elseBody != null
    }

    companion object {
        const val DEFAULT_REPEAT = 2
        const val MIN_REPEAT = 1
        const val MAX_REPEAT = 9

        /** Builds a program from blocks written with ids already, e.g. a reference solution. */
        fun of(vararg blocks: Block): Program {
            val list = blocks.toList()
            return Program(list, nextId = (maxId(list) ?: 0) + 1)
        }

        private fun maxId(blocks: List<Block>): Int? = blocks.maxOfOrNull { block ->
            maxOf(block.id, maxId(block.bodyOrEmpty()) ?: 0, maxId((block as? IfBlock)?.elseBody.orEmpty()) ?: 0)
        }

        private fun countBlocks(blocks: List<Block>): Int = blocks.sumOf { block ->
            1 + countBlocks(block.bodyOrEmpty()) + countBlocks((block as? IfBlock)?.elseBody.orEmpty())
        }

        private fun findIn(blocks: List<Block>, id: Int): Block? {
            for (block in blocks) {
                if (block.id == id) return block
                findIn(block.bodyOrEmpty(), id)?.let { return it }
                findIn((block as? IfBlock)?.elseBody.orEmpty(), id)?.let { return it }
            }
            return null
        }

        private fun update(blocks: List<Block>, id: Int, change: (Block) -> Block): List<Block> = blocks.map { block ->
            if (block.id == id) {
                change(block)
            } else {
                block.mapChildren { children -> update(children, id, change) }
            }
        }

        private fun removeFrom(blocks: List<Block>, id: Int): List<Block> = blocks
            .filter { it.id != id }
            .map { block -> block.mapChildren { children -> removeFrom(children, id) } }
    }
}

/** The body of a container block, or an empty list for a command. */
fun Block.bodyOrEmpty(): List<Block> = when (this) {
    is CommandBlock -> emptyList()
    is RepeatBlock -> body
    is IfBlock -> body
    is WhileBlock -> body
}

private fun Block.withBody(body: List<Block>): Block = when (this) {
    is CommandBlock -> this
    is RepeatBlock -> copy(body = body)
    is IfBlock -> copy(body = body)
    is WhileBlock -> copy(body = body)
}

/** Applies [change] to every child list (body and else-branch) of this block. */
private fun Block.mapChildren(change: (List<Block>) -> List<Block>): Block = when (this) {
    is CommandBlock -> this
    is RepeatBlock -> copy(body = change(body))
    is IfBlock -> copy(body = change(body), elseBody = elseBody?.let(change))
    is WhileBlock -> copy(body = change(body))
}
