package com.example.nativestarter.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nativestarter.ui.screens.HomeScreen
import com.example.nativestarter.ui.screens.SettingsScreen
import com.example.nativestarter.viewmodel.AppViewModel

/** Route names for the two top-level destinations. */
object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val navItems = listOf(
    NavItem(Routes.HOME, "Home", Icons.Filled.Home),
    NavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

/**
 * App shell: bottom navigation bar plus the navigation host.
 * Counter and settings state live in one [AppViewModel], scoped to the activity.
 */
@Composable
fun AppRoot(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(viewModel = viewModel)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
