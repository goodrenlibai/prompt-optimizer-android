package com.promptoptimizer.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.padding
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
import com.promptoptimizer.ui.screens.FavoritesScreen
import com.promptoptimizer.ui.screens.HistoryScreen
import com.promptoptimizer.ui.screens.HomeScreen
import com.promptoptimizer.ui.screens.ProScreen
import com.promptoptimizer.ui.screens.TemplatesScreen
import com.promptoptimizer.ui.screens.TestevalScreen
import com.promptoptimizer.ui.screens.WorkspaceScreen
import com.promptoptimizer.ui.viewmodel.MainViewModel

object Routes {
    const val HOME = "home"
    const val WORKSPACE = "workspace"
    const val PRO = "pro"
    const val TESTEVAL = "testeval"
    const val FAVORITES = "favorites"
    const val TEMPLATES = "templates"
    const val HISTORY = "history"
}

data class BottomItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun NavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()

    val bottomItems = listOf(
        BottomItem(Routes.HOME, "首页", Icons.Filled.Home),
        BottomItem(Routes.WORKSPACE, "工作台", Icons.Filled.Edit),
        BottomItem(Routes.PRO, "专业", Icons.AutoMirrored.Filled.Chat),
        BottomItem(Routes.TESTEVAL, "测试评估", Icons.Filled.Assessment),
        BottomItem(Routes.FAVORITES, "收藏", Icons.Filled.Star)
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    for (item in bottomItems) {
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(viewModel, onOpenTemplates = { navController.navigate(Routes.TEMPLATES) }, onOpenHistory = { navController.navigate(Routes.HISTORY) })
            }
            composable(Routes.WORKSPACE) { WorkspaceScreen(viewModel) }
            composable(Routes.PRO) { ProScreen(viewModel) }
            composable(Routes.TESTEVAL) { TestevalScreen(viewModel) }
            composable(Routes.FAVORITES) { FavoritesScreen(viewModel) }
            composable(Routes.TEMPLATES) { TemplatesScreen(viewModel) }
            composable(Routes.HISTORY) { HistoryScreen(viewModel) }
        }
    }
}
