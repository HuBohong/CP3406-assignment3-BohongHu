package au.edu.jcu.codequest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * App theme. Dynamic (wallpaper-based) colour is intentionally not used so that the
 * correct/incorrect feedback colours keep the same, tested contrast on every device.
 */
@Composable
fun CodeQuestTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
