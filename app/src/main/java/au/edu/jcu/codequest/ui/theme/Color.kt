package au.edu.jcu.codequest.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Palette built around Kotlin's own violet, because the course teaches Kotlin.
// Green means "passed", amber means XP and stars, red means "not yet".
// Every text colour used on these surfaces meets WCAG AA contrast (4.5:1 for body text).

private val Violet = Color(0xFF7F52FF)
private val VioletDeep = Color(0xFF5A32D6)
private val VioletInk = Color(0xFF2A0E7A)
private val VioletSoft = Color(0xFFECE6FF)
private val VioletLight = Color(0xFFC2ADFF)

private val Ink = Color(0xFF1D1838)
private val InkSoft = Color(0xFF5E5878)
private val Paper = Color(0xFFF7F5FF)
private val Line = Color(0xFFE4E0F2)

val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletSoft,
    onPrimaryContainer = VioletInk,
    inversePrimary = VioletLight,
    secondary = InkSoft,
    onSecondary = Color.White,
    secondaryContainer = Line,
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF1D2),
    onTertiaryContainer = Color(0xFF3D2800),
    error = Color(0xFFC62F35),
    onError = Color.White,
    errorContainer = Color(0xFFFDE4E4),
    onErrorContainer = Color(0xFF5C0F13),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9E5F5),
    onSurfaceVariant = InkSoft,
    surfaceTint = Violet,
    inverseSurface = Color(0xFF2F2A4A),
    inverseOnSurface = Color(0xFFF3F0FF),
    outline = Color(0xFF8C86A8),
    outlineVariant = Line,
    surfaceBright = Paper,
    surfaceDim = Color(0xFFDCD8EA),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFAFF),
    surfaceContainer = Color(0xFFF2EFFC),
    surfaceContainerHigh = Color(0xFFECE8FA),
    surfaceContainerHighest = Color(0xFFE6E2F5),
)

val DarkColors = darkColorScheme(
    primary = VioletLight,
    onPrimary = VioletInk,
    primaryContainer = Color(0xFF4C22C9),
    onPrimaryContainer = VioletSoft,
    inversePrimary = Violet,
    secondary = Color(0xFFC9C3E0),
    onSecondary = Color(0xFF2F2A4A),
    secondaryContainer = Color(0xFF3A3456),
    onSecondaryContainer = Color(0xFFE4E0F2),
    tertiary = Color(0xFFFFC94D),
    onTertiary = Color(0xFF3A2600),
    tertiaryContainer = Color(0xFF5C3F00),
    onTertiaryContainer = Color(0xFFFFE6B0),
    error = Color(0xFFFFB4B0),
    onError = Color(0xFF5C0F13),
    errorContainer = Color(0xFF7A1F24),
    onErrorContainer = Color(0xFFFFDAD7),
    background = Color(0xFF131026),
    onBackground = Color(0xFFE8E4FA),
    surface = Color(0xFF131026),
    onSurface = Color(0xFFE8E4FA),
    surfaceVariant = Color(0xFF3A3456),
    onSurfaceVariant = Color(0xFFC2BCD8),
    surfaceTint = VioletLight,
    inverseSurface = Color(0xFFE8E4FA),
    inverseOnSurface = Color(0xFF2F2A4A),
    outline = Color(0xFF8C86A8),
    outlineVariant = Color(0xFF3A3456),
    surfaceBright = Color(0xFF39334F),
    surfaceDim = Color(0xFF131026),
    surfaceContainerLowest = Color(0xFF0E0B1F),
    surfaceContainerLow = Color(0xFF1A1631),
    surfaceContainer = Color(0xFF1E1A36),
    surfaceContainerHigh = Color(0xFF282340),
    surfaceContainerHighest = Color(0xFF332D4C),
)

/** Face and lower edge of a chunky, "pressable" element. */
@Immutable
data class ChunkyColors(
    val face: Color,
    val edge: Color,
    val content: Color,
    val border: Color = face,
)

/** Colour for one unit of the course: its banner, lesson nodes and planet. */
@Immutable
data class UnitColors(
    val fill: Color,
    val edge: Color,
)

/**
 * Colours Material 3 has no role for: answer states, XP, the code editor and the space map.
 * Read them with [CodeQuestTheme.colors].
 */
@Immutable
data class ExtendedColors(
    val primaryButton: ChunkyColors,
    val successButton: ChunkyColors,
    val dangerButton: ChunkyColors,
    val xpButton: ChunkyColors,
    val neutralButton: ChunkyColors,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val xp: Color,
    val onXpSurface: Color,
    val lockFill: Color,
    val lockEdge: Color,
    val lockInk: Color,
    val cardEdge: Color,
    val units: List<UnitColors>,
    val editor: EditorColors,
    val space: SpaceColors,
)

@Immutable
data class EditorColors(
    val background: Color,
    val text: Color,
    val lineNumber: Color,
    val keyword: Color,
    val number: Color,
    val string: Color,
    val function: Color,
    val comment: Color,
    val currentLine: Color,
    val currentLineMarker: Color,
)

@Immutable
data class SpaceColors(
    val top: Color,
    val middle: Color,
    val bottom: Color,
    val nebulaPink: Color,
    val nebulaBlue: Color,
    val glass: Color,
    val glassBorder: Color,
    val text: Color,
    val textSoft: Color,
    val trail: Color,
    val lockFill: Color,
    val lockEdge: Color,
    val lockInk: Color,
    val navBar: Color,
)

private val EditorPalette = EditorColors(
    background = Color(0xFF221D3B),
    text = Color(0xFFECEAF7),
    lineNumber = Color(0xFF8D87AE),
    keyword = Color(0xFFFF9452),
    number = Color(0xFF8FB8FF),
    string = Color(0xFF7EE0A8),
    function = Color(0xFFC9A7FF),
    comment = Color(0xFF8C86A8),
    currentLine = Color(0x477F52FF),
    currentLineMarker = Color(0xFFB79CFF),
)

private val SpacePalette = SpaceColors(
    top = Color(0xFF0E0930),
    middle = Color(0xFF1B1052),
    bottom = Color(0xFF2A1468),
    nebulaPink = Color(0xFFC83DD8),
    nebulaBlue = Color(0xFF2F7CF6),
    glass = Color(0x14FFFFFF),
    glassBorder = Color(0x29FFFFFF),
    text = Color.White,
    textSoft = Color(0xFFCFC8F5),
    trail = Color(0x59FFFFFF),
    lockFill = Color(0xFF3A3170),
    lockEdge = Color(0xFF221B4D),
    lockInk = Color(0xFF9F98C9),
    navBar = Color(0xFF150E3D),
)

private val UnitPalette = listOf(
    UnitColors(fill = Color(0xFF6B3EF0), edge = Color(0xFF4C22C9)),
    UnitColors(fill = Color(0xFF1F6FE5), edge = Color(0xFF1652B0)),
    UnitColors(fill = Color(0xFF0F8A5F), edge = Color(0xFF0A6646)),
    UnitColors(fill = Color(0xFFB02CC4), edge = Color(0xFF821F92)),
)

val LightExtendedColors = ExtendedColors(
    primaryButton = ChunkyColors(face = Violet, edge = VioletDeep, content = Color.White),
    successButton = ChunkyColors(face = Color(0xFF0E8C5E), edge = Color(0xFF086245), content = Color.White),
    dangerButton = ChunkyColors(face = Color(0xFFD93A40), edge = Color(0xFFA92A30), content = Color.White),
    xpButton = ChunkyColors(face = Color(0xFFA86A00), edge = Color(0xFF7A4D00), content = Color.White),
    neutralButton = ChunkyColors(face = Color.White, edge = Line, content = VioletDeep, border = Line),
    success = Color(0xFF0B7A52),
    successContainer = Color(0xFFDDF6EC),
    onSuccessContainer = Color(0xFF0B4F37),
    xp = Color(0xFFFFB31A),
    onXpSurface = Color(0xFF8A5A00),
    lockFill = Color(0xFFE6E2F0),
    lockEdge = Color(0xFFCCC6DD),
    lockInk = Color(0xFF7E7898),
    cardEdge = Line,
    units = UnitPalette,
    editor = EditorPalette,
    space = SpacePalette,
)

val DarkExtendedColors = LightExtendedColors.copy(
    primaryButton = ChunkyColors(face = Violet, edge = Color(0xFF3F1FA8), content = Color.White),
    neutralButton = ChunkyColors(
        face = Color(0xFF282340),
        edge = Color(0xFF1A1631),
        content = VioletLight,
        border = Color(0xFF3A3456),
    ),
    success = Color(0xFF6FE0B0),
    successContainer = Color(0xFF0F3D2C),
    onSuccessContainer = Color(0xFFC6F3E0),
    onXpSurface = Color(0xFFFFC94D),
    lockFill = Color(0xFF332D4C),
    lockEdge = Color(0xFF221D38),
    lockInk = Color(0xFF9F98C9),
    cardEdge = Color(0xFF3A3456),
)
