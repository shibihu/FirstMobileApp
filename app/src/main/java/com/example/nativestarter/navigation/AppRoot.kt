package com.example.nativestarter.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VideoLibrary
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
import com.example.nativestarter.ui.screens.AiToolsScreen
import com.example.nativestarter.ui.screens.EditorScreen
import com.example.nativestarter.ui.screens.HomeScreen
import com.example.nativestarter.ui.screens.MediaLibraryScreen
import com.example.nativestarter.ui.screens.SettingsScreen
import com.example.nativestarter.viewmodel.AppViewModel

/** Route names for the app's primary destinations. */
object Routes {
    const val HOME = "dashboard"
    const val MEDIA = "media"
    const val EDITOR = "editor"
    const val AI = "ai"
    const val SETTINGS = "settings"
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val navItems = listOf(
    NavItem(Routes.HOME, "Dashboard", Icons.Filled.Home),
    NavItem(Routes.MEDIA, "Media", Icons.Filled.VideoLibrary),
    NavItem(Routes.EDITOR, "Editor", Icons.Filled.Timeline),
    NavItem(Routes.AI, "AI", Icons.Filled.AutoAwesome),
    NavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

/**
 * App shell: bottom navigation bar plus the navigation host.
 * The workspace state is scoped to the activity via [AppViewModel].
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
                HomeScreen(
                    viewModel = viewModel,
                    onContinueEditing = { navController.navigate(Routes.EDITOR) },
                    onImportMedia = { navController.navigate(Routes.MEDIA) },
                    onOpenAi = { navController.navigate(Routes.AI) },
                )
            }
            composable(Routes.MEDIA) {
                MediaLibraryScreen(viewModel = viewModel)
            }
            composable(Routes.EDITOR) {
                EditorScreen(viewModel = viewModel)
            }
            composable(Routes.AI) {
                AiToolsScreen(viewModel = viewModel)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
