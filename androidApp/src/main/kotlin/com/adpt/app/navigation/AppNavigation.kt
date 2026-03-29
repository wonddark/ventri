package com.adpt.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.adpt.app.AdptApplication
import com.adpt.app.ui.items.ItemsScreen
import com.adpt.app.ui.overview.OverviewScreen
import com.adpt.app.ui.shopping.ShoppingScreen
import com.adpt.app.ui.stock.StockScreen

private sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Overview : Screen("overview", "Overview", Icons.Default.Home)
    data object Shopping : Screen("shopping", "Shopping", Icons.Default.ShoppingCart)
    data object Stock : Screen("stock", "Stock", Icons.Default.Refresh)
    data object Items : Screen("items", "Items", Icons.Default.Menu)

    companion object {
        val tabs = listOf(Overview, Shopping, Stock, Items)
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as AdptApplication
    val pendingNav by app.pendingNavTarget.collectAsStateWithLifecycle()

    // Consume any pending target at first composition to avoid a flash of the Overview screen.
    val startDestination = remember {
        app.pendingNavTarget.value?.also { app.pendingNavTarget.value = null }
            ?: Screen.Overview.route
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Handle navigation triggered while the app is already running (e.g. onNewIntent).
    LaunchedEffect(pendingNav) {
        pendingNav?.let { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            app.pendingNavTarget.value = null
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                Screen.tabs.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Overview.route) { OverviewScreen() }
            composable(Screen.Shopping.route) { ShoppingScreen() }
            composable(Screen.Stock.route) { StockScreen() }
            composable(Screen.Items.route) { ItemsScreen() }
        }
    }
}
