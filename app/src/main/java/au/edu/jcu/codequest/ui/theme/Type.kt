package au.edu.jcu.codequest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import au.edu.jcu.codequest.R

/** Poppins: rounded geometric letters that read as friendly and game-like (SIL Open Font License). */
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

/** Used only for real code, so code always looks different from the interface around it. */
val CodeFont = FontFamily(
    Font(R.font.dejavu_sans_mono, FontWeight.Normal),
    Font(R.font.dejavu_sans_mono_bold, FontWeight.Bold),
)

private val base = Typography()

private fun TextStyle.poppins(weight: FontWeight) = copy(fontFamily = Poppins, fontWeight = weight)

/** Headings are bold and slightly tighter; body text keeps the Material sizes for readability. */
val CodeQuestTypography = Typography(
    displayLarge = base.displayLarge.poppins(FontWeight.Bold),
    displayMedium = base.displayMedium.poppins(FontWeight.Bold),
    displaySmall = base.displaySmall.poppins(FontWeight.Bold),
    headlineLarge = base.headlineLarge.poppins(FontWeight.Bold).copy(letterSpacing = (-0.4).sp),
    headlineMedium = base.headlineMedium.poppins(FontWeight.Bold).copy(letterSpacing = (-0.3).sp),
    headlineSmall = base.headlineSmall.poppins(FontWeight.Bold).copy(letterSpacing = (-0.2).sp),
    titleLarge = base.titleLarge.poppins(FontWeight.Bold).copy(fontSize = 22.sp, lineHeight = 29.sp),
    titleMedium = base.titleMedium.poppins(FontWeight.Bold),
    titleSmall = base.titleSmall.poppins(FontWeight.Bold),
    bodyLarge = base.bodyLarge.poppins(FontWeight.Normal).copy(lineHeight = 24.sp),
    bodyMedium = base.bodyMedium.poppins(FontWeight.Normal).copy(lineHeight = 21.sp),
    bodySmall = base.bodySmall.poppins(FontWeight.Normal),
    labelLarge = base.labelLarge.poppins(FontWeight.Bold),
    labelMedium = base.labelMedium.poppins(FontWeight.Medium),
    labelSmall = base.labelSmall.poppins(FontWeight.Medium),
)
