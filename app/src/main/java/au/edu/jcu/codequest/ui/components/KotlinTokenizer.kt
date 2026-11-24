package au.edu.jcu.codequest.ui.components

/** What a piece of Kotlin source is, for syntax colouring. */
enum class TokenKind { KEYWORD, NUMBER, STRING, FUNCTION, COMMENT, PLAIN }

data class CodeToken(val text: String, val kind: TokenKind)

/**
 * A small, forgiving tokenizer for the one-line snippets shown in lessons.
 *
 * It is not a full Kotlin lexer: it only needs to colour keywords, numbers, strings, comments
 * and function calls well enough to make short examples easier to read. Joining the tokens'
 * text always gives back the original line exactly.
 */
object KotlinTokenizer {

    private val keywords = setOf(
        "val", "var", "fun", "if", "else", "when", "for", "in", "while", "do", "return",
        "null", "true", "false", "until", "downTo", "step", "class", "data", "object",
        "is", "as", "break", "continue",
    )

    private val tokenPattern = Regex(
        listOf(
            """//.*""", // comment to end of line
            """"(?:\\.|[^"\\])*"?""", // string literal (an unclosed one runs to the end)
            """\d+(?:\.\d+)?[fFL]?""", // number
            """[A-Za-z_][A-Za-z0-9_]*""", // identifier or keyword
            """\s+""", // whitespace
            """.""", // any other single character
        ).joinToString("|") { "($it)" },
    )

    fun tokenize(line: String): List<CodeToken> {
        val matches = tokenPattern.findAll(line).toList()
        return matches.mapIndexed { index, match ->
            val text = match.value
            val kind = when {
                match.groups[1] != null -> TokenKind.COMMENT
                match.groups[2] != null -> TokenKind.STRING
                match.groups[3] != null -> TokenKind.NUMBER
                match.groups[4] != null -> when {
                    text in keywords -> TokenKind.KEYWORD
                    nextNonSpace(matches, index) == "(" -> TokenKind.FUNCTION
                    else -> TokenKind.PLAIN
                }
                else -> TokenKind.PLAIN
            }
            CodeToken(text, kind)
        }
    }

    private fun nextNonSpace(matches: List<MatchResult>, index: Int): String? =
        matches.drop(index + 1).firstOrNull { it.value.isNotBlank() }?.value
}
