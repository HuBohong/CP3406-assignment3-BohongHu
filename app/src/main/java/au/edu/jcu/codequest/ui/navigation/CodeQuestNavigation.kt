package au.edu.jcu.codequest.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import au.edu.jcu.codequest.ui.home.HomeRoute
import au.edu.jcu.codequest.ui.lesson.LessonRoute
import au.edu.jcu.codequest.ui.mission.MissionRoute
import au.edu.jcu.codequest.ui.onboarding.OnboardingRoute
import au.edu.jcu.codequest.ui.result.ResultScreen
import au.edu.jcu.codequest.ui.settings.SettingsRoute
import au.edu.jcu.codequest.ui.stats.StatsRoute
import au.edu.jcu.codequest.ui.theme.CodeQuestTheme

/** Width from which a side navigation rail replaces the bottom bar (tablets, landscape). */
private val RAIL_MIN_WIDTH = 600.dp

/**
 * Root of the UI: the navigation graph plus adaptive top-level navigation.
 * The bar or rail is only shown on the three top-level tabs, so a lesson gets the full screen.
 */
@Composable
fun CodeQuestNavigation(startDestination: String) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showTopLevelNavigation = TopLevelDestination.entries.any { currentDestination.isOn(it) }
    // The Learn map sits on a dark space background, so the bar or rail turns dark with it.
    val onSpace = currentDestination.isOn(TopLevelDestination.HOME)
    val barColors = topLevelBarColors(onSpace)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= RAIL_MIN_WIDTH) {
            Row(Modifier.fillMaxSize()) {
                if (showTopLevelNavigation) {
                    NavigationRail(containerColor = barColors.container) {
                        TopLevelDestination.entries.forEach { destination ->
                            NavigationRailItem(
                                selected = currentDestination.isOn(destination),
                                onClick = { navController.navigateToTopLevel(destination) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(stringResource(destination.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = barColors.selectedContent,
                                    selectedTextColor = barColors.selectedText,
                                    indicatorColor = barColors.indicator,
                                    unselectedIconColor = barColors.unselected,
                                    unselectedTextColor = barColors.unselected,
                                ),
                            )
                        }
                    }
                }
                CodeQuestNavHost(navController, startDestination, Modifier.weight(1f))
            }
        } else {
            Scaffold(
                // Each screen has its own Scaffold that handles the system bars.
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showTopLevelNavigation) {
                        NavigationBar(containerColor = barColors.container) {
                            TopLevelDestination.entries.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentDestination.isOn(destination),
                                    onClick = { navController.navigateToTopLevel(destination) },
                                    icon = { Icon(destination.icon, contentDescription = null) },
                                    label = { Text(stringResource(destination.labelRes)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = barColors.selectedContent,
                                        selectedTextColor = barColors.selectedText,
                                        indicatorColor = barColors.indicator,
                                        unselectedIconColor = barColors.unselected,
                                        unselectedTextColor = barColors.unselected,
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                CodeQuestNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun CodeQuestNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(Routes.ONBOARDING) {
            OnboardingRoute(
                onFinished = {
                    // Onboarding is removed from the back stack: Back from Home leaves the app.
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeRoute(
                onOpenLesson = { lessonId -> navController.navigate(Routes.lesson(lessonId)) },
                onOpenMission = { missionId -> navController.navigate(Routes.mission(missionId)) },
                onOpenBonus = { navController.navigate(Routes.lesson(Routes.BONUS_ID)) },
            )
        }
        composable(
            route = Routes.MISSION,
            arguments = listOf(navArgument(Routes.ARG_MISSION_ID) { type = NavType.StringType }),
        ) {
            MissionRoute(onExit = { navController.popBackStack() })
        }
        composable(Routes.STATS) { StatsRoute() }
        composable(Routes.SETTINGS) { SettingsRoute() }
        composable(
            route = Routes.LESSON,
            arguments = listOf(navArgument(Routes.ARG_LESSON_ID) { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = Routes.LESSON_DEEP_LINK }),
        ) {
            LessonRoute(
                onExit = {
                    // A lesson opened from a deep link has no Home underneath it yet.
                    if (!navController.popBackStack(Routes.HOME, inclusive = false)) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LESSON) { inclusive = true }
                        }
                    }
                },
                onFinished = { result ->
                    // Replace the lesson with its result so Back does not reopen a finished quiz.
                    navController.navigate(Routes.result(result)) {
                        popUpTo(Routes.LESSON) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument(Routes.ARG_LESSON_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_STARS) { type = NavType.IntType },
                navArgument(Routes.ARG_XP) { type = NavType.IntType },
                navArgument(Routes.ARG_FIRST_TRY) { type = NavType.IntType },
                navArgument(Routes.ARG_TOTAL) { type = NavType.IntType },
            ),
        ) { entry ->
            val arguments = entry.arguments
            val lessonId = arguments?.getString(Routes.ARG_LESSON_ID).orEmpty()
            ResultScreen(
                stars = arguments?.getInt(Routes.ARG_STARS) ?: 0,
                xp = arguments?.getInt(Routes.ARG_XP) ?: 0,
                firstTryCorrect = arguments?.getInt(Routes.ARG_FIRST_TRY) ?: 0,
                total = arguments?.getInt(Routes.ARG_TOTAL) ?: 0,
                isBonus = lessonId == Routes.BONUS_ID,
                onDone = {
                    if (!navController.popBackStack(Routes.HOME, inclusive = false)) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.RESULT) { inclusive = true }
                        }
                    }
                },
                onPractiseAgain = {
                    navController.navigate(Routes.lesson(lessonId)) {
                        popUpTo(Routes.RESULT) { inclusive = true }
                    }
                },
            )
        }
    }
}

/** Colours for the bar or rail, dark over the space map and light everywhere else. */
private data class TopLevelBarColors(
    val container: Color,
    val indicator: Color,
    val selectedContent: Color,
    val selectedText: Color,
    val unselected: Color,
)

@Composable
private fun topLevelBarColors(onSpace: Boolean): TopLevelBarColors {
    val scheme = MaterialTheme.colorScheme
    return if (onSpace) {
        TopLevelBarColors(
            container = CodeQuestTheme.colors.space.navBar,
            indicator = CodeQuestTheme.colors.primaryButton.face,
            selectedContent = Color.White,
            selectedText = Color.White,
            unselected = Color(0xFFB4ADDC),
        )
    } else {
        TopLevelBarColors(
            container = scheme.surfaceContainerLowest,
            indicator = scheme.primaryContainer,
            selectedContent = scheme.onPrimaryContainer,
            selectedText = scheme.primary,
            unselected = scheme.onSurfaceVariant,
        )
    }
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.route == destination.route } == true

/**
 * Switches tab the way Material guidance recommends: one copy of each tab on the back stack,
 * and each tab's state (such as scroll position) is saved and restored.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
