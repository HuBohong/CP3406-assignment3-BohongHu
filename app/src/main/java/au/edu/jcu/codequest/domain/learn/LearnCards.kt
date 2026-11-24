package au.edu.jcu.codequest.domain.learn

/**
 * Interactive teaching cards shown at the start of a lesson, before the practice questions.
 * They are defined in code (see [LearnCatalog]) because each one comes with a small
 * simulation that computes what the code does for any value the learner picks.
 */
sealed interface LearnCard {
    val id: String
    val title: String
    val body: String
}

/** One editable value in a playground's code, picked from [options]. */
data class SlotSpec(
    val name: String,
    val options: List<String>,
    val initial: Int,
) {
    init {
        require(initial in options.indices) { "Initial option out of range for $name" }
    }
}

/** A "guess first" question shown before the learner may run or change the code. */
data class Prediction(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
)

/** What a scene beside the code shows. Each kind is drawn by its own composable. */
sealed interface Scene {
    data class Fuel(val level: Int, val capacity: Int = 100) : Scene
    data class Types(val values: List<TypedValue>) : Scene
    data class Radio(val message: String?) : Scene
    data class Shields(val asteroids: Int, val raised: Boolean) : Scene
    data class PlanetColor(val planet: String, val color: String) : Scene
    data class Launch(val rockets: List<Int>) : Scene
    data class Countdown(val display: String, val launched: Boolean) : Scene
}

/** A variable and the type Kotlin infers for it; a null [type] means it does not compile. */
data class TypedValue(val name: String, val value: String?, val type: String?)

/** What running a playground's code produced. Line numbers are 1-based. */
data class RunResult(
    val output: List<String>,
    val scene: Scene,
    val error: String? = null,
    val highlightedLine: Int? = null,
    val note: String? = null,
)

/**
 * "Try it": code with editable values. Every change re-runs [evaluate] and the scene reacts
 * straight away. `{0}`, `{1}` … in [code] mark where [slots] go.
 */
class Playground(
    override val id: String,
    override val title: String,
    override val body: String,
    val code: List<String>,
    val slots: List<SlotSpec>,
    val prediction: Prediction? = null,
    val goal: String? = null,
    private val run: (List<String>) -> RunResult,
    private val goalCheck: (List<String>) -> Boolean = { false },
) : LearnCard {

    fun initialValues(): List<String> = slots.map { it.options[it.initial] }

    fun valuesAt(indices: List<Int>): List<String> = slots.mapIndexed { i, slot -> slot.options[indices[i]] }

    fun evaluate(values: List<String>): RunResult = run(values)

    fun isGoalMet(values: List<String>): Boolean = goal != null && goalCheck(values)

    /** The code with every slot filled in, as plain text. */
    fun render(values: List<String>): List<String> = code.map { line ->
        PLACEHOLDER.replace(line) { match -> values[match.groupValues[1].toInt()] }
    }

    companion object {
        val PLACEHOLDER = Regex("""\{(\d+)\}""")
    }
}

/** A variable as shown in the "Watch it" panel. */
data class Variable(
    val name: String,
    val value: String,
    val isVal: Boolean,
    val previous: String? = null,
)

/** The program just after [line] (1-based) ran. [output] is everything printed so far. */
data class TraceStep(
    val line: Int,
    val variables: List<Variable>,
    val output: List<String>,
    val scene: Scene? = null,
)

/** "Watch it": step through a program one line at a time. */
data class StepThrough(
    override val id: String,
    override val title: String,
    override val body: String,
    val code: List<String>,
    val steps: List<TraceStep>,
) : LearnCard

/** "Fix the bug": find the line that causes the problem. [buggyLine] is 1-based. */
data class FixTheBug(
    override val id: String,
    override val title: String,
    override val body: String,
    val code: List<String>,
    val buggyLine: Int,
    val fixedLine: String,
    val problem: String,
    val wrongLineHint: String,
    val explanation: String,
    val fixedOutput: List<String>,
) : LearnCard {
    val fixedCode: List<String> get() = code.mapIndexed { i, line -> if (i + 1 == buggyLine) fixedLine else line }
}
