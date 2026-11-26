package au.edu.jcu.codequest.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import au.edu.jcu.codequest.domain.mission.BlockCollection
import au.edu.jcu.codequest.domain.mission.Sandbox
import au.edu.jcu.codequest.ui.flight.FlightRoute
import au.edu.jcu.codequest.ui.home.HomeRoute
import au.edu.jcu.codequest.ui.lesson.LessonRoute
import au.edu.jcu.codequest.ui.mission.MissionRoute
import au.edu.jcu.codequest.ui.onboarding.OnboardingRoute
import au.edu.jcu.codequest.ui.result.ResultScreen
import au.edu.jcu.codequest.ui.result.storyBeatFor
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
fun CodeQuestNavigation(startDestination: Destination) {
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
                    NavigationRail(
                        containerColor = barColors.container,
                        // Keeps the labels clear of a camera cutout on the rail's side in landscape.
                        windowInsets = NavigationRailDefaults.windowInsets
                            .union(WindowInsets.displayCutout.only(WindowInsetsSides.Start)),
                    ) {
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
    startDestination: Destination,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable<Destination.Onboarding> {
            OnboardingRoute(
                onFinished = {
                    // Onboarding is removed from the back stack: Back from Home leaves the app.
                    navController.navigate(Destination.Home) {
                        popUpTo<Destination.Onboarding> { inclusive = true }
                    }
                },
            )
        }
        composable<Destination.Home> {
            HomeRoute(
                onOpenLesson = { lessonId -> navController.navigate(Destination.Lesson(lessonId)) },
                onOpenMission = { missionId -> navController.navigate(Destination.Mission(missionId)) },
                onOpenBonus = { navController.navigate(Destination.Lesson(Routes.BONUS_ID)) },
                onOpenFlight = { flightId -> navController.navigate(Destination.Flight(flightId)) },
                onOpenSandbox = { navController.navigate(Destination.Mission(Sandbox.ID)) },
            )
        }
        composable<Destination.Flight> {
            FlightRoute(onExit = { navController.popBackStack() })
        }
        composable<Destination.Mission> {
            MissionRoute(onExit = { navController.popBackStack() })
        }
        composable<Destination.Stats> { StatsRoute() }
        composable<Destination.Settings> { SettingsRoute() }
        composable<Destination.Lesson>(
            deepLinks = listOf(navDeepLink<Destination.Lesson>(basePath = Routes.LESSON_DEEP_LINK_BASE)),
        ) {
            LessonRoute(
                onExit = { navController.backToHome<Destination.Lesson>() },
                onFinished = { result ->
                    // Replace the lesson with its result so Back does not reopen a finished quiz.
                    navController.navigate(Destination.Result.from(result)) {
                        popUpTo<Destination.Lesson> { inclusive = true }
                    }
                },
            )
        }
        composable<Destination.Result> { entry ->
            val result = entry.toRoute<Destination.Result>()
            val storyBeat = storyBeatFor(result.lessonId)?.takeIf { result.firstTime }
            ResultScreen(
                stars = result.stars,
                xp = result.xp,
                firstTryCorrect = result.firstTry,
                total = result.total,
                isBonus = result.lessonId == Routes.BONUS_ID,
                newBlockLabels = if (result.firstTime) {
                    BlockCollection.rewardFor(result.lessonId).map { it.label }
                } else {
                    emptyList()
                },
                storyBeat = if (storyBeat != null) stringResource(storyBeat) else null,
                onDone = { navController.backToHome<Destination.Result>() },
                onPractiseAgain = {
                    navController.navigate(Destination.Lesson(result.lessonId)) {
                        popUpTo<Destination.Result> { inclusive = true }
                    }
                },
            )
        }
    }
}

/**
 * Returns to the map. A lesson opened from a deep link has no Home underneath it yet, so in that
 * case Home is opened and the screen being left is removed from the back stack.
 */
private inline fun <reified Leaving : Destination> NavHostController.backToHome() {
    if (!popBackStack<Destination.Home>(inclusive = false)) {
        navigate(Destination.Home) {
            popUpTo<Leaving> { inclusive = true }
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
    this?.hierarchy?.any { it.hasRoute(destination.destination::class) } == true

/**
 * Switches tab the way Material guidance recommends: one copy of each tab on the back stack,
 * and each tab's state (such as scroll position) is saved and restored.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.destination) {
        popUpTo<Destination.Home> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
