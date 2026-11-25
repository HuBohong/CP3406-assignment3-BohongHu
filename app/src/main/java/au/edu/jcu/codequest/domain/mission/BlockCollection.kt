package au.edu.jcu.codequest.domain.mission

/**
 * The code blocks Byte recovers from the crash. Finishing a lesson for the first time adds its
 * blocks to the learner's toolbox; missions and the free-build planet only offer blocks that
 * have been collected. Collection is worked out from completed lessons, so it needs no storage
 * of its own and can never disagree with the learner's progress.
 */
object BlockCollection {

    /** Blocks each lesson hands over, in path order. Lessons not listed add no new blocks. */
    val rewards: Map<String, List<PaletteBlock>> = linkedMapOf(
        "basics-1" to listOf(PaletteBlock.Act(Action.MOVE), PaletteBlock.Act(Action.COLLECT)),
        "basics-2" to listOf(PaletteBlock.Act(Action.TURN_LEFT), PaletteBlock.Act(Action.TURN_RIGHT)),
        "flow-1" to listOf(PaletteBlock.If, PaletteBlock.IfElse),
        "flow-2" to listOf(PaletteBlock.Repeat, PaletteBlock.While),
        "fun-1" to listOf(PaletteBlock.Act(Action.MOVE_UP), PaletteBlock.Act(Action.MOVE_DOWN)),
        "fun-2" to listOf(PaletteBlock.Act(Action.STAY)),
    )

    /** Every collectable block, in the order they are collected. */
    val all: List<PaletteBlock> = rewards.values.flatten()

    fun rewardFor(lessonId: String): List<PaletteBlock> = rewards[lessonId].orEmpty()

    /** The lesson that hands over [block], or null if no lesson does. */
    fun lessonFor(block: PaletteBlock): String? = rewards.entries.firstOrNull { block in it.value }?.key

    fun collected(completedLessonIds: Set<String>): Set<PaletteBlock> =
        rewards.filterKeys { it in completedLessonIds }.values.flatten().toSet()
}
