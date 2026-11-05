package com.abeerapps.kmpexpensesplitter.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abeerapps.kmpexpensesplitter.feature.onboarding.OnboardingScreen
import com.abeerapps.kmpexpensesplitter.feature.registration.RegistrationScreen
import com.abeerapps.kmpexpensesplitter.ui.MainShell
import com.abeerapps.kmpexpensesplitter.ui.PlaceholderScreen

/**
 * App navigation graph. Features expose plain callbacks; only this file knows
 * about routes, so features stay decoupled from each other and from Navigation.
 */
@Composable
fun AppNavHost(
    startDestination: AppDestination,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<AppDestination.Onboarding> {
            OnboardingScreen(
                onNavigateToRegistration = {
                    navController.navigate(AppDestination.Registration) { launchSingleTop = true }
                },
                onNavigateToHome = {
                    // Onboarding must not be reachable via back once left.
                    navController.navigate(AppDestination.Home) {
                        popUpTo<AppDestination.Onboarding> { inclusive = true }
                    }
                },
            )
        }
        composable<AppDestination.Registration> {
            RegistrationScreen(
                onRegistered = {
                    // Clear onboarding + registration: back from Home exits the app.
                    navController.navigate(AppDestination.Home) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
        composable<AppDestination.Home> {
            MainShell(
                onCreateGroup = { navController.navigate(AppDestination.CreateGroup) },
                onGroupClick = { id -> navController.navigate(AppDestination.GroupDetails(id)) },
            )
        }
        composable<AppDestination.CreateGroup> {
            PlaceholderScreen(title = "Create group", onBack = navController::navigateUp)
        }
        composable<AppDestination.GroupDetails> {
            PlaceholderScreen(title = "Group details", onBack = navController::navigateUp)
        }
    }
}
