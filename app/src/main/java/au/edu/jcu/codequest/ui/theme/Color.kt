package au.edu.jcu.codequest.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Deep teal for focus, warm coral for rewards. Each pair below meets WCAG AA contrast
// for body text (4.5:1 or better).

private val Teal10 = Color(0xFF00201F)
private val Teal20 = Color(0xFF003735)
private val Teal30 = Color(0xFF00504D)
private val Teal40 = Color(0xFF1F6966)
private val Teal80 = Color(0xFF8CD3CE)
private val Teal90 = Color(0xFFA8F0EA)

private val Slate10 = Color(0xFF051F1E)
private val Slate30 = Color(0xFF324B49)
private val Slate40 = Color(0xFF4A6361)
private val Slate80 = Color(0xFFB1CCC9)
private val Slate90 = Color(0xFFCCE8E5)

private val Coral10 = Color(0xFF3B0906)
private val Coral30 = Color(0xFF73342B)
private val Coral40 = Color(0xFF904B40)
private val Coral80 = Color(0xFFFFB4A8)
private val Coral90 = Color(0xFFFFDAD4)

private val Sand98 = Color(0xFFF7FAF8)
private val Ink10 = Color(0xFF191C1C)
private val Ink90 = Color(0xFFE0E3E2)
private val Night = Color(0xFF101414)

val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Slate40,
    onSecondary = Color.White,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate10,
    tertiary = Coral40,
    onTertiary = Color.White,
    tertiaryContainer = Coral90,
    onTertiaryContainer = Coral10,
    background = Sand98,
    onBackground = Ink10,
    surface = Sand98,
    onSurface = Ink10,
)

val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Slate80,
    onSecondary = Slate10,
    secondaryContainer = Slate30,
    onSecondaryContainer = Slate90,
    tertiary = Coral80,
    onTertiary = Coral10,
    tertiaryContainer = Coral30,
    onTertiaryContainer = Coral90,
    background = Night,
    onBackground = Ink90,
    surface = Night,
    onSurface = Ink90,
)
