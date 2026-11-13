package au.edu.jcu.codequest

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.codequest.domain.model.ThemeMode
import au.edu.jcu.codequest.ui.navigation.CodeQuestNavigation
import au.edu.jcu.codequest.ui.navigation.Routes
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme
import dagger.hilt.android.AndroidEntryPoint

/** The single activity. Everything on screen is Compose, hosted by [CodeQuestNavigation]. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Keep status and navigation bar icons readable when the in-app theme
            // differs from the system theme.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }

            CodeQuestTheme(darkTheme = darkTheme) {
                if (uiState.isLoading) {
                    // Settings load in a few milliseconds; show the background, not a wrong screen.
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                } else {
                    // Decided once: finishing onboarding navigates instead of rebuilding the graph.
                    val startDestination = rememberSaveable {
                        if (uiState.onboardingDone) Routes.HOME else Routes.ONBOARDING
                    }
                    CodeQuestNavigation(startDestination = startDestination)
                }
            }
        }
    }

    private companion object {
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
