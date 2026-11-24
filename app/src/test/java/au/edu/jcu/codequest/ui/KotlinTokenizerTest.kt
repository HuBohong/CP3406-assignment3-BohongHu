package au.edu.jcu.codequest.ui

import au.edu.jcu.codequest.ui.components.KotlinTokenizer
import au.edu.jcu.codequest.ui.components.TokenKind
import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinTokenizerTest {

    private fun kinds(line: String) =
        KotlinTokenizer.tokenize(line).filter { it.text.isNotBlank() }.map { it.text to it.kind }

    @Test
    fun keywordsNumbersAndPlainNamesAreSeparated() {
        assertEquals(
            listOf(
                "var" to TokenKind.KEYWORD,
                "score" to TokenKind.PLAIN,
                "=" to TokenKind.PLAIN,
                "10" to TokenKind.NUMBER,
            ),
            kinds("var score = 10"),
        )
    }

    @Test
    fun anIdentifierBeforeAParenthesisIsAFunctionCall() {
        val tokens = kinds("println(score)")

        assertEquals("println" to TokenKind.FUNCTION, tokens.first())
        assertEquals("score" to TokenKind.PLAIN, tokens[2])
    }

    @Test
    fun stringsKeepTemplatesAndEscapedQuotesInside() {
        val tokens = kinds("""println("Hi, ${'$'}name \"friend\"")""")

        assertEquals(TokenKind.STRING, tokens[2].second)
        assertEquals(""""Hi, ${'$'}name \"friend\""""", tokens[2].first)
    }

    @Test
    fun commentsRunToTheEndOfTheLine() {
        val tokens = kinds("val x = 1 // set x")

        assertEquals("// set x" to TokenKind.COMMENT, tokens.last())
    }

    @Test
    fun joiningTheTokensGivesBackTheLineExactly() {
        listOf(
            "for (i in 10 downTo 1 step 3) {",
            "    val label = if (age >= 18) \"adult\" else \"minor\"",
            "println(name ?: \"Guest\")",
            "val pi = 3.14f",
            "\"unclosed string",
            "",
        ).forEach { line ->
            assertEquals(line, KotlinTokenizer.tokenize(line).joinToString("") { it.text })
        }
    }
}
