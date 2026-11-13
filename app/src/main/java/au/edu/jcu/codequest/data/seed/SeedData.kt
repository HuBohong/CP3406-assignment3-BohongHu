package au.edu.jcu.codequest.data.seed

import au.edu.jcu.codequest.domain.model.Lesson
import au.edu.jcu.codequest.domain.model.Question
import au.edu.jcu.codequest.domain.model.QuestionType

/**
 * The built-in Kotlin course. It ships inside the app and is copied into Room on first launch,
 * so every core lesson works without a network connection or an account.
 */
object SeedData {

    val lessons: List<Lesson> = listOf(
        Lesson("basics-1", 1, "Kotlin basics", 1, "Values and variables", "Store data with val and var"),
        Lesson("basics-2", 1, "Kotlin basics", 2, "Types and strings", "Type inference and string templates"),
        Lesson("flow-1", 2, "Making decisions", 1, "If and when", "Choose what your code does"),
        Lesson("flow-2", 2, "Making decisions", 2, "Loops and ranges", "Repeat work with for and while"),
        Lesson("fun-1", 3, "Functions", 1, "Writing functions", "Parameters, return values and defaults"),
        Lesson("fun-2", 3, "Functions", 2, "Lambdas", "Pass behaviour around as a value"),
        Lesson("data-1", 4, "Working with data", 1, "Lists and maps", "Group values into collections"),
        Lesson("data-2", 4, "Working with data", 2, "Null safety", "Handle missing values without crashes"),
    )

    val questions: List<Question> = variables() + typesAndStrings() + conditionals() + loops() +
        functions() + lambdas() + collections() + nullSafety()

    private fun variables(): List<Question> {
        val lesson = "basics-1"
        val topic = "Variables"
        return listOf(
            choice(
                lesson, 1, topic,
                prompt = "Which keyword declares a read-only variable in Kotlin?",
                options = listOf("var", "val", "let", "const"),
                correct = 1,
                explanation = "val creates a read-only reference. Use var when the value needs to change.",
            ),
            choice(
                lesson, 2, topic,
                prompt = "What does this program print?",
                code = code("var score = 10", "score = score + 5", "println(score)"),
                options = listOf("10", "105", "15", "Compile error"),
                correct = 2,
                explanation = "score is a var, so it can be reassigned: 10 + 5 is 15.",
            ),
            choice(
                lesson, 3, topic,
                prompt = "What happens when you compile this code?",
                code = code("val name = \"Ada\"", "name = \"Grace\""),
                options = listOf(
                    "name becomes \"Grace\"",
                    "Compile error: a val cannot be reassigned",
                    "The app crashes at runtime",
                    "name becomes null",
                ),
                correct = 1,
                explanation = "A val can only be assigned once. The compiler catches the mistake before the app runs.",
            ),
            choice(
                lesson, 4, topic,
                prompt = "Fill the blank so that this code compiles.",
                code = code("____ lives = 3", "lives = lives - 1"),
                options = listOf("var", "val", "fun", "const"),
                correct = 0,
                explanation = "lives changes on the second line, so it has to be declared with var.",
            ),
            order(
                lesson, 5, topic,
                prompt = "Put the lines in order so the program prints 8.",
                lines = listOf("fun main() {", "    var total = 5", "    total += 3", "    println(total)", "}"),
                explanation = "Declare the variable first, change it, then print it, all inside main().",
            ),
        )
    }

    private fun typesAndStrings(): List<Question> {
        val lesson = "basics-2"
        val topic = "Types and strings"
        return listOf(
            choice(
                lesson, 1, topic,
                prompt = "Which type does Kotlin infer for pi?",
                code = code("val pi = 3.14"),
                options = listOf("Double", "Float", "Int", "String"),
                correct = 0,
                explanation = "A decimal literal is a Double by default. A Float needs an f suffix, as in 3.14f.",
            ),
            choice(
                lesson, 2, topic,
                prompt = "What does this program print?",
                code = code("val name = \"Sam\"", "println(\"Hi, \$name!\")"),
                options = listOf("Hi, \$name!", "Hi, name!", "Compile error", "Hi, Sam!"),
                correct = 3,
                explanation = "A string template replaces \$name with the value of the variable.",
            ),
            choice(
                lesson, 3, topic,
                prompt = "What does this program print?",
                code = code("val items = 3", "println(\"Total: \${items * 2}\")"),
                options = listOf("Total: items * 2", "Total: 6", "Total: 32", "Total: 3 * 2"),
                correct = 1,
                explanation = "Curly braces let a template evaluate a whole expression: 3 * 2 is 6.",
            ),
            choice(
                lesson, 4, topic,
                prompt = "Which expression turns the string \"42\" into an Int?",
                options = listOf("Int(\"42\")", "(Int) \"42\"", "\"42\".toInt()", "\"42\" as Int"),
                correct = 2,
                explanation = "Kotlin converts between types with functions such as toInt(), toDouble() and toString().",
            ),
            order(
                lesson, 5, topic,
                prompt = "Put the lines in order so the program prints the length of the full name.",
                lines = listOf(
                    "val first = \"Ada\"",
                    "val full = first + \" Lovelace\"",
                    "println(full.length)",
                ),
                explanation = "Each line uses a value created on the line before it.",
            ),
        )
    }

    private fun conditionals(): List<Question> {
        val lesson = "flow-1"
        val topic = "Conditionals"
        return listOf(
            choice(
                lesson, 1, topic,
                prompt = "What does this program print?",
                code = code(
                    "val temp = 30",
                    "if (temp > 25) {",
                    "    println(\"Hot\")",
                    "} else {",
                    "    println(\"Mild\")",
                    "}",
                ),
                options = listOf("Hot", "Mild", "Hot Mild", "Nothing"),
                correct = 0,
                explanation = "30 > 25 is true, so only the first branch runs.",
            ),
            choice(
                lesson, 2, topic,
                prompt = "In Kotlin, if is an expression. What is the value of label?",
                code = code("val age = 16", "val label = if (age >= 18) \"adult\" else \"minor\""),
                options = listOf("\"adult\"", "\"minor\"", "true", "Compile error"),
                correct = 1,
                explanation = "16 >= 18 is false, so the if expression produces the else value.",
            ),
            choice(
                lesson, 3, topic,
                prompt = "What does this program print?",
                code = code(
                    "val day = 3",
                    "val name = when (day) {",
                    "    1 -> \"Mon\"",
                    "    2 -> \"Tue\"",
                    "    3 -> \"Wed\"",
                    "    else -> \"?\"",
                    "}",
                    "println(name)",
                ),
                options = listOf("Mon", "Tue", "?", "Wed"),
                correct = 3,
                explanation = "when compares day with each branch in turn and uses the first match.",
            ),
            choice(
                lesson, 4, topic,
                prompt = "Which operator means logical AND?",
                options = listOf("||", "!", "&&", "=="),
                correct = 2,
                explanation = "a && b is true only when both sides are true. || is OR and ! is NOT.",
            ),
            order(
                lesson, 5, topic,
                prompt = "Put the lines in order so the program prints Pass.",
                lines = listOf(
                    "val score = 72",
                    "val grade = if (score >= 50) \"Pass\" else \"Fail\"",
                    "println(grade)",
                ),
                explanation = "score must exist before the if expression can test it.",
            ),
        )
    }

    private fun loops(): List<Question> {
        val lesson = "flow-2"
        val topic = "Loops"
        return listOf(
            choice(
                lesson, 1, topic,
                prompt = "What does this program print?",
                code = code("for (i in 1..3) {", "    print(i)", "}"),
                options = listOf("123", "012", "1234", "3"),
                correct = 0,
                explanation = "The range 1..3 includes both ends: 1, 2 and 3.",
            ),
            choice(
                lesson, 2, topic,
                prompt = "How many times does the loop body run?",
                code = code("for (i in 0 until 5) {", "    println(i)", "}"),
                options = listOf("4", "5", "6", "It never stops"),
                correct = 1,
                explanation = "until leaves out the end value, so i goes 0, 1, 2, 3, 4: five times.",
            ),
            choice(
                lesson, 3, topic,
                prompt = "What does this program print?",
                code = code("var n = 3", "while (n > 0) {", "    n--", "}", "println(n)"),
                options = listOf("3", "1", "0", "-1"),
                correct = 2,
                explanation = "The loop keeps subtracting 1 while n is above 0, so it stops when n reaches 0.",
            ),
            choice(
                lesson, 4, topic,
                prompt = "Which values does i take in this loop?",
                code = code("for (i in 10 downTo 1 step 3) {", "    println(i)", "}"),
                options = listOf("10, 9, 8, 7", "1, 4, 7, 10", "10, 7, 4", "10, 7, 4, 1"),
                correct = 3,
                explanation = "downTo counts backwards and step 3 skips three at a time, ending on 1.",
            ),
            order(
                lesson, 5, topic,
                prompt = "Put the lines in order to add up the numbers 1 to 4 and print the result.",
                lines = listOf("var sum = 0", "for (i in 1..4) {", "    sum += i", "}", "println(sum)"),
                explanation = "Start the total at 0, add each number inside the loop, then print after the loop ends.",
            ),
        )
    }

    private fun functions(): List<Question> {
        val lesson = "fun-1"
        val topic = "Functions"
        return listOf(
            choice(
                lesson, 1, topic,
                prompt = "Which keyword declares a function in Kotlin?",
                options = listOf("def", "fun", "function", "func"),
                correct = 1,
                explanation = "Kotlin functions start with fun, for example fun main().",
            ),
            choice(
                lesson, 2, topic,
                prompt = "What does this program print?",
                code = code("fun double(x: Int): Int {", "    return x * 2", "}", "println(double(4))"),
                options = listOf("8", "4", "6", "42"),
                correct = 0,
                explanation = "The argument 4 is passed in as x, and the function returns 4 * 2.",
            ),
            choice(
                lesson, 3, topic,
                prompt = "What is the return type of greet?",
                code = code("fun greet(name: String) = \"Hello, \$name\""),
                options = listOf("Unit", "Any", "String", "Nothing"),
                correct = 2,
                explanation = "For a single-expression function the compiler infers the type from the expression.",
            ),
            choice(
                lesson, 4, topic,
                prompt = "What does this program print?",
                code = code(
                    "fun power(base: Int, exp: Int = 2): Int {",
                    "    var result = 1",
                    "    repeat(exp) { result *= base }",
                    "    return result",
                    "}",
                    "println(power(3))",
                ),
                options = listOf("3", "6", "27", "9"),
                correct = 3,
                explanation = "exp is not passed, so its default value 2 is used: 3 * 3 is 9.",
            ),
            order(
                lesson, 5, topic,
                prompt = "Put the lines in order to define area and print the area of a 3 by 4 rectangle.",
                lines = listOf("fun area(w: Int, h: Int): Int {", "    return w * h", "}", "println(area(3, 4))"),
                explanation = "The function is declared first: header, body, closing brace. Then it is called.",
            ),
        )
    }

    private fun lambdas(): List<Question> {
        val lesson = "fun-2"
        val topic = "Lambdas"
        return listOf(
            choice(
                lesson, 1, topic,
                prompt = "What does this program print?",
                code = code("val square = { x: Int -> x * x }", "println(square(5))"),
                options = listOf("10", "25", "55", "Compile error"),
                correct = 1,
                explanation = "square holds a lambda. Calling it with 5 evaluates 5 * 5.",
            ),
            choice(
                lesson, 2, topic,
                prompt = "A lambda with exactly one parameter can use an implicit name for it. Which one?",
                options = listOf("it", "this", "self", "arg"),
                correct = 0,
                explanation = "it refers to the single parameter, as in list.map { it * 2 }.",
            ),
            choice(
                lesson, 3, topic,
                prompt = "What does this program print?",
                code = code("val nums = listOf(1, 2, 3)", "println(nums.map { it * 2 })"),
                options = listOf("[1, 2, 3]", "6", "[2, 4, 6]", "[1, 4, 9]"),
                correct = 2,
                explanation = "map builds a new list by applying the lambda to every element.",
            ),
            choice(
                lesson, 4, topic,
                prompt = "What does this program print?",
                code = code("val nums = listOf(1, 2, 3, 4)", "println(nums.filter { it % 2 == 0 })"),
                options = listOf("[1, 3]", "2", "[1, 2, 3, 4]", "[2, 4]"),
                correct = 3,
                explanation = "filter keeps the elements for which the lambda returns true: the even numbers.",
            ),
            order(
                lesson, 5, topic,
                prompt = "Put the lines in order so the program prints [6, 2, 3].",
                lines = listOf(
                    "val words = listOf(\"kotlin\", \"is\", \"fun\")",
                    "val lengths = words.map { it.length }",
                    "println(lengths)",
                ),
                explanation = "Create the list, transform it with map, then print the new list.",
            ),
        )
    }

    private fun collections(): List<Question> {
        val lesson = "data-1"
        val topic = "Collections"
        return listOf(
            choice(
                lesson, 1, topic,
                prompt = "What does this program print?",
                code = code("val fruits = listOf(\"apple\", \"pear\", \"fig\")", "println(fruits[1])"),
                options = listOf("apple", "pear", "fig", "An index error"),
                correct = 1,
                explanation = "List indexes start at 0, so index 1 is the second element.",
            ),
            choice(
                lesson, 2, topic,
                prompt = "Which call creates a list that you can add items to later?",
                options = listOf("listOf(1, 2)", "arrayOf(1, 2)", "mutableListOf(1, 2)", "setOf(1, 2)"),
                correct = 2,
                explanation = "listOf gives a read-only list. mutableListOf gives one with add() and remove().",
            ),
            choice(
                lesson, 3, topic,
                prompt = "What does this program print?",
                code = code("val ages = mapOf(\"Ann\" to 20, \"Bo\" to 31)", "println(ages[\"Bo\"])"),
                options = listOf("31", "20", "Bo", "null"),
                correct = 0,
                explanation = "A map stores key-value pairs. Looking up the key \"Bo\" returns its value.",
            ),
            choice(
                lesson, 4, topic,
                prompt = "What does this program print?",
                code = code("val nums = mutableListOf(1, 2)", "nums.add(3)", "println(nums.size)"),
                options = listOf("2", "Compile error", "4", "3"),
                correct = 3,
                explanation = "val stops nums from pointing at a different list, but the list itself can still change.",
            ),
            order(
                lesson, 5, topic,
                prompt = "Put the lines in order so the program prints 245.",
                lines = listOf("val scores = mutableListOf(70, 85)", "scores.add(90)", "println(scores.sum())"),
                explanation = "The third score has to be added before the total is calculated.",
            ),
        )
    }

    private fun nullSafety(): List<Question> {
        val lesson = "data-2"
        val topic = "Null safety"
        return listOf(
            choice(
                lesson, 1, topic,
                prompt = "Which declaration allows the variable to hold null?",
                options = listOf(
                    "val name: String = null",
                    "val name: String? = null",
                    "val name = String(null)",
                    "val name: Null = null",
                ),
                correct = 1,
                explanation = "A question mark after the type makes it nullable. A plain String can never be null.",
            ),
            choice(
                lesson, 2, topic,
                prompt = "What does this program print?",
                code = code("val name: String? = null", "println(name?.length)"),
                options = listOf("0", "A NullPointerException", "null", "Compile error"),
                correct = 2,
                explanation = "The safe call ?. returns null instead of crashing when name is null.",
            ),
            choice(
                lesson, 3, topic,
                prompt = "What does this program print?",
                code = code("val name: String? = null", "println(name ?: \"Guest\")"),
                options = listOf("Guest", "null", "name", "Compile error"),
                correct = 0,
                explanation = "The Elvis operator ?: supplies a fallback when the left side is null.",
            ),
            choice(
                lesson, 4, topic,
                prompt = "What does the !! operator do?",
                options = listOf(
                    "Safely returns null",
                    "Compares two values",
                    "Negates a Boolean twice",
                    "Treats a value as non-null and throws if it is null",
                ),
                correct = 3,
                explanation = "!! throws a NullPointerException on null, so prefer ?. or ?: when you can.",
            ),
            order(
                lesson, 5, topic,
                prompt = "Put the lines in order so the program prints Anonymous.",
                lines = listOf(
                    "val nickname: String? = null",
                    "val shown = nickname ?: \"Anonymous\"",
                    "println(shown)",
                ),
                explanation = "nickname is null, so the Elvis operator falls back to \"Anonymous\".",
            ),
        )
    }

    private fun code(vararg lines: String): String = lines.joinToString(separator = "\n")

    private fun choice(
        lessonId: String,
        number: Int,
        topic: String,
        prompt: String,
        options: List<String>,
        correct: Int,
        explanation: String,
        code: String? = null,
    ) = Question(
        id = "$lessonId-q$number",
        lessonId = lessonId,
        topic = topic,
        type = QuestionType.CHOICE,
        prompt = prompt,
        code = code,
        options = options,
        correctIndex = correct,
        explanation = explanation,
    )

    private fun order(
        lessonId: String,
        number: Int,
        topic: String,
        prompt: String,
        lines: List<String>,
        explanation: String,
    ) = Question(
        id = "$lessonId-q$number",
        lessonId = lessonId,
        topic = topic,
        type = QuestionType.ORDER,
        prompt = prompt,
        options = lines,
        explanation = explanation,
    )
}
