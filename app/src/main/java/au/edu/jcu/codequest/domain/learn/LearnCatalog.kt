package au.edu.jcu.codequest.domain.learn

/**
 * Teaching cards for the first lessons. Each lesson repairs one of Byte's ship systems,
 * which is what the scenes show: the fuel tank, the radio, the shields and the launch pad.
 */
object LearnCatalog {

    fun forLesson(lessonId: String): List<LearnCard> = when (lessonId) {
        "basics-1" -> listOf(watchFuel, fuelPlayground, valBug)
        "basics-2" -> listOf(typesPlayground, watchRadio, typeBug)
        "flow-1" -> listOf(shieldsPlayground, whenPlayground, assignBug)
        "flow-2" -> listOf(launchPlayground, watchCountdown, offByOneBug)
        else -> emptyList()
    }

    val all: List<LearnCard> get() = listOf("basics-1", "basics-2", "flow-1", "flow-2").flatMap(::forLesson)

    // ---------- Lesson 1: values and variables (fuel tank) ----------

    val watchFuel = StepThrough(
        id = "basics-1-watch",
        title = "A var can change, a val can't",
        body = "Byte's fuel tank is empty. Step through the program and watch each variable appear.",
        code = listOf(
            "val ship = \"Nova\"",
            "var fuel = 40",
            "fuel = fuel + 35",
            "println(fuel)",
        ),
        steps = listOf(
            TraceStep(1, listOf(Variable("ship", "\"Nova\"", isVal = true)), emptyList(), Scene.Fuel(0)),
            TraceStep(
                2,
                listOf(Variable("ship", "\"Nova\"", isVal = true), Variable("fuel", "40", isVal = false)),
                emptyList(),
                Scene.Fuel(40),
            ),
            TraceStep(
                3,
                listOf(Variable("ship", "\"Nova\"", isVal = true), Variable("fuel", "75", isVal = false, previous = "40")),
                emptyList(),
                Scene.Fuel(75),
            ),
            TraceStep(
                4,
                listOf(Variable("ship", "\"Nova\"", isVal = true), Variable("fuel", "75", isVal = false)),
                listOf("75"),
                Scene.Fuel(75),
            ),
        ),
    )

    val fuelPlayground = Playground(
        id = "basics-1-try",
        title = "Fill the tank",
        body = "Change the starting fuel and how much is added. The tank holds 100.",
        code = listOf(
            "var fuel = {0}",
            "fuel = fuel + {1}",
            "println(fuel)",
        ),
        slots = listOf(
            SlotSpec("Starting fuel", (0..100 step 10).map(Int::toString), initial = 4),
            SlotSpec("Added", (0..60 step 5).map(Int::toString), initial = 7),
        ),
        prediction = Prediction(
            question = "Before you run it: what will println(fuel) show?",
            options = listOf("40", "35", "75", "4035"),
            correctIndex = 2,
        ),
        goal = "Make the tank exactly full: 100.",
        run = { values ->
            val fuel = values[0].toInt() + values[1].toInt()
            RunResult(
                output = listOf(fuel.toString()),
                scene = Scene.Fuel(fuel),
                note = if (fuel > 100) "The tank overflows! Kotlin is happy with any Int, but the tank only holds 100." else null,
            )
        },
        goalCheck = { values -> values[0].toInt() + values[1].toInt() == 100 },
    )

    val valBug = FixTheBug(
        id = "basics-1-fix",
        title = "Why won't it compile?",
        body = "Byte tried to top up the tank, but the code doesn't compile. Tap the line that causes the problem.",
        code = listOf(
            "val fuel = 40",
            "fuel = fuel + 10",
            "println(fuel)",
        ),
        buggyLine = 1,
        fixedLine = "var fuel = 40",
        problem = "Compile error on line 2: Val cannot be reassigned",
        wrongLineHint = "The error is reported on line 2, but the cause is how fuel was declared.",
        explanation = "fuel changes later, so it must be declared with var. A val can be assigned only once.",
        fixedOutput = listOf("50"),
    )

    // ---------- Lesson 2: types and strings (radio) ----------

    val typesPlayground = Playground(
        id = "basics-2-try",
        title = "Kotlin works out the type",
        body = "You never wrote a type, yet every value has one. Pick a value and watch the types change.",
        code = listOf(
            "val planets = {0}",
            "val trip = planets * 2",
            "println(trip)",
        ),
        slots = listOf(SlotSpec("planets", listOf("3", "3.5", "\"3\""), initial = 1)),
        prediction = Prediction(
            question = "Before you run it: which type does Kotlin give planets = 3.5?",
            options = listOf("Int", "Double", "Float", "String"),
            correctIndex = 1,
        ),
        run = { values ->
            when (values[0]) {
                "3" -> RunResult(
                    output = listOf("6"),
                    scene = Scene.Types(listOf(TypedValue("planets", "3", "Int"), TypedValue("trip", "6", "Int"))),
                )
                "3.5" -> RunResult(
                    output = listOf("7.0"),
                    scene = Scene.Types(listOf(TypedValue("planets", "3.5", "Double"), TypedValue("trip", "7.0", "Double"))),
                )
                else -> RunResult(
                    output = emptyList(),
                    scene = Scene.Types(listOf(TypedValue("planets", "\"3\"", "String"), TypedValue("trip", null, null))),
                    error = "Compile error on line 2: a String can't be multiplied by 2",
                    highlightedLine = 2,
                    note = "Quotes make \"3\" text, not a number. Kotlin catches this before the app ever runs.",
                )
            }
        },
    )

    val watchRadio = StepThrough(
        id = "basics-2-watch",
        title = "Build a message with a template",
        body = "A \$ inside a string drops a value into the text. Step through to send Byte's radio message.",
        code = listOf(
            "val name = \"Byte\"",
            "val planets = 3",
            "val msg = \"Hi \$name, \$planets planets to go\"",
            "println(msg)",
        ),
        steps = listOf(
            TraceStep(1, listOf(Variable("name", "\"Byte\"", isVal = true)), emptyList(), Scene.Radio(null)),
            TraceStep(
                2,
                listOf(Variable("name", "\"Byte\"", isVal = true), Variable("planets", "3", isVal = true)),
                emptyList(),
                Scene.Radio(null),
            ),
            TraceStep(
                3,
                listOf(
                    Variable("name", "\"Byte\"", isVal = true),
                    Variable("planets", "3", isVal = true),
                    Variable("msg", "\"Hi Byte, 3 planets to go\"", isVal = true),
                ),
                emptyList(),
                Scene.Radio(null),
            ),
            TraceStep(
                4,
                listOf(
                    Variable("name", "\"Byte\"", isVal = true),
                    Variable("planets", "3", isVal = true),
                    Variable("msg", "\"Hi Byte, 3 planets to go\"", isVal = true),
                ),
                listOf("Hi Byte, 3 planets to go"),
                Scene.Radio("Hi Byte, 3 planets to go"),
            ),
        ),
    )

    val typeBug = FixTheBug(
        id = "basics-2-fix",
        title = "Type mismatch",
        body = "The radio should print 4, but the code doesn't compile. Tap the line that causes the problem.",
        code = listOf(
            "val planets: Int = \"3\"",
            "println(planets + 1)",
        ),
        buggyLine = 1,
        fixedLine = "val planets: Int = 3",
        problem = "Compile error on line 1: Type mismatch, expected Int but found String",
        wrongLineHint = "Look at the value given to planets. Does it match the type Int?",
        explanation = "\"3\" in quotes is a String. An Int needs the number itself: 3.",
        fixedOutput = listOf("4"),
    )

    // ---------- Lesson 3: if and when (shields) ----------

    val shieldsPlayground = Playground(
        id = "flow-1-try",
        title = "Shields up?",
        body = "The highlighted line is the branch that runs. Change the asteroid count and the limit.",
        code = listOf(
            "val asteroids = {0}",
            "if (asteroids > {1}) {",
            "    raiseShields()",
            "} else {",
            "    cruise()",
            "}",
        ),
        slots = listOf(
            SlotSpec("asteroids", (0..10).map(Int::toString), initial = 7),
            SlotSpec("limit", (0..10).map(Int::toString), initial = 5),
        ),
        prediction = Prediction(
            question = "Before you run it: 7 asteroids, limit 5. Which line runs?",
            options = listOf("raiseShields()", "cruise()", "Both", "Neither"),
            correctIndex = 0,
        ),
        goal = "Find values where the asteroids equal the limit. Which branch runs then?",
        run = { values ->
            val asteroids = values[0].toInt()
            val raised = asteroids > values[1].toInt()
            RunResult(
                output = listOf(if (raised) "Shields up" else "Cruising"),
                scene = Scene.Shields(asteroids, raised),
                highlightedLine = if (raised) 3 else 5,
            )
        },
        goalCheck = { values -> values[0] == values[1] },
    )

    private val planetColours = mapOf("Mars" to "red", "Neptune" to "blue", "Earth" to "green")

    val whenPlayground = Playground(
        id = "flow-1-when",
        title = "when picks one branch",
        body = "when compares the value with each branch, top to bottom, and runs the first match.",
        code = listOf(
            "val planet = {0}",
            "val colour = when (planet) {",
            "    \"Mars\" -> \"red\"",
            "    \"Neptune\" -> \"blue\"",
            "    \"Earth\" -> \"green\"",
            "    else -> \"grey\"",
            "}",
            "println(colour)",
        ),
        slots = listOf(SlotSpec("planet", listOf("\"Mars\"", "\"Neptune\"", "\"Earth\"", "\"Pluto\""), initial = 0)),
        run = { values ->
            val planet = values[0].trim('"')
            val colour = planetColours[planet] ?: "grey"
            val line = when (planet) {
                "Mars" -> 3
                "Neptune" -> 4
                "Earth" -> 5
                else -> 6
            }
            RunResult(
                output = listOf(colour),
                scene = Scene.PlanetColor(planet, colour),
                highlightedLine = line,
                note = if (line == 6) "No branch matched, so else ran. when needs else when it produces a value." else null,
            )
        },
    )

    val assignBug = FixTheBug(
        id = "flow-1-fix",
        title = "Compare, don't assign",
        body = "Launch control should check the fuel, but this doesn't compile. Tap the line that causes the problem.",
        code = listOf(
            "val fuel = 100",
            "if (fuel = 100) {",
            "    println(\"Ready to launch\")",
            "}",
        ),
        buggyLine = 2,
        fixedLine = "if (fuel == 100) {",
        problem = "Compile error on line 2: Assignments are not expressions",
        wrongLineHint = "Look at the condition inside if ( ). Is it comparing or assigning?",
        explanation = "= puts a value into a variable. == asks whether two values are equal, which is what if needs.",
        fixedOutput = listOf("Ready to launch"),
    )

    // ---------- Lesson 4: loops and ranges (launch pad) ----------

    /** The rockets a `for (i in 1<operator><end> step <step>)` loop launches. */
    fun launchedRockets(operator: String, end: Int, step: Int): List<Int> {
        val range = if (operator == "..") 1..end else 1 until end
        return (range step step).toList()
    }

    val launchPlayground = Playground(
        id = "flow-2-try",
        title = "One launch per number",
        body = "The loop body runs once for every number in the range. Try .. and until, the end and the step.",
        code = listOf(
            "for (i in 1{0}{1} step {2}) {",
            "    launch(i)",
            "}",
        ),
        slots = listOf(
            SlotSpec("range", listOf("..", " until "), initial = 0),
            SlotSpec("end", (0..9).map(Int::toString), initial = 5),
            SlotSpec("step", listOf("1", "2", "3"), initial = 0),
        ),
        prediction = Prediction(
            question = "Before you run it: how many rockets will for (i in 1..5) launch?",
            options = listOf("4", "5", "6"),
            correctIndex = 1,
        ),
        goal = "Launch only the odd-numbered rockets 1, 3, 5 and 7.",
        run = { values ->
            val rockets = launchedRockets(values[0].trim(), values[1].toInt(), values[2].toInt())
            RunResult(
                output = if (rockets.isEmpty()) emptyList() else listOf(rockets.joinToString(" ")),
                scene = Scene.Launch(rockets),
                note = if (rockets.isEmpty()) "Empty range! The start is past the end, so the body never runs." else null,
            )
        },
        goalCheck = { values ->
            launchedRockets(values[0].trim(), values[1].toInt(), values[2].toInt()) == listOf(1, 3, 5, 7)
        },
    )

    /** Builds the trace of a while-loop countdown from [start], so it is always consistent. */
    fun countdownTrace(start: Int): List<TraceStep> {
        val steps = mutableListOf<TraceStep>()
        val output = mutableListOf<String>()
        var n = start
        fun vars(previous: String? = null) = listOf(Variable("n", n.toString(), isVal = false, previous = previous))
        steps += TraceStep(1, vars(), output.toList(), Scene.Countdown(n.toString(), launched = false))
        while (true) {
            steps += TraceStep(2, vars(), output.toList(), Scene.Countdown(n.toString(), launched = false))
            if (n <= 0) break
            output += n.toString()
            steps += TraceStep(3, vars(), output.toList(), Scene.Countdown(n.toString(), launched = false))
            val before = n.toString()
            n--
            steps += TraceStep(4, vars(previous = before), output.toList(), Scene.Countdown(n.toString(), launched = false))
        }
        output += "Liftoff!"
        steps += TraceStep(6, vars(), output.toList(), Scene.Countdown("Liftoff!", launched = true))
        return steps
    }

    val watchCountdown = StepThrough(
        id = "flow-2-watch",
        title = "Count down with while",
        body = "while checks its condition before every lap. Watch n shrink until the condition is false.",
        code = listOf(
            "var n = 3",
            "while (n > 0) {",
            "    println(n)",
            "    n--",
            "}",
            "println(\"Liftoff!\")",
        ),
        steps = countdownTrace(3),
    )

    val offByOneBug = FixTheBug(
        id = "flow-2-fix",
        title = "One rocket too many",
        body = "Byte wants exactly 5 rockets, numbered 0 to 4, but 6 took off. Tap the line that causes the problem.",
        code = listOf(
            "for (i in 0..5) {",
            "    launch(i)",
            "}",
        ),
        buggyLine = 1,
        fixedLine = "for (i in 0 until 5) {",
        problem = "Output: 0 1 2 3 4 5 (that's 6 rockets)",
        wrongLineHint = "launch(i) is fine. Which numbers does the range produce?",
        explanation = "0..5 includes 5. until leaves the end out, so 0 until 5 gives exactly 0, 1, 2, 3, 4. This is called an off-by-one error.",
        fixedOutput = listOf("0 1 2 3 4"),
    )
}
