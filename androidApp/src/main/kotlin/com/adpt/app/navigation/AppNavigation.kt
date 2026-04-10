package com.adpt.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adpt.app.AdptApplication
import com.adpt.app.ui.design.components.AdptNavBar
import com.adpt.app.ui.design.components.AdptNavItem
import com.adpt.app.ui.design.components.AdptScaffold
import com.adpt.app.ui.items.ItemsScreen
import com.adpt.app.ui.overview.OverviewScreen
import com.adpt.app.ui.preferences.PreferencesScreen
import com.adpt.app.ui.shopping.ShoppingScreen
import com.adpt.app.ui.stock.StockScreen
import kotlinx.coroutines.flow.filterNotNull

private sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Overview : Screen("overview", "Overview", Icons.Default.AvTimer)
    data object Shopping : Screen("shopping", "Shopping", Icons.Default.ShoppingCart)
    data object Stock : Screen("stock", "Stock", Icons.Default.Inventory)
    data object Items : Screen("items", "Items", Icons.Default.Category)

    companion object {
        val tabs = listOf(Overview, Shopping, Stock, Items)
    }
}

private val navItems = Screen.tabs.map { screen ->
    AdptNavItem(route = screen.route, label = screen.label, icon = screen.icon)
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as AdptApplication

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        app.pendingNavTarget.filterNotNull().collect { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            app.pendingNavTarget.value = null
        }
    }

    val selectionModeActive = backStackEntry?.arguments?.getBoolean("selectionMode") ?: false
    val isTopLevelRoute = Screen.tabs.any { screen ->
        currentRoute == screen.route ||
            (screen == Screen.Items &&
                currentRoute?.startsWith("${screen.route}?") == true &&
                !selectionModeActive)
    }

    AdptScaffold(
        modifier = modifier,
        bottomBar = {
            if (isTopLevelRoute) {
                AdptNavBar(
                    items = navItems,
                    currentRoute = currentRoute,
                    onItemSelected = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Overview.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Overview.route) {
                OverviewScreen(onOpenSettings = { navController.navigate("preferences") })
            }
            composable(Screen.Shopping.route) {
                ShoppingScreen(navController = navController)
            }
            composable(Screen.Stock.route) {
                StockScreen()
            }
            composable("preferences") {
                PreferencesScreen(onNavigateUp = { navController.navigateUp() })
            }
            composable(
                route = "${Screen.Items.route}?selectionMode={selectionMode}",
                arguments = listOf(
                    navArgument("selectionMode") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                ),
            ) { ItemsScreen(navController = navController) }
        }
    }
}
